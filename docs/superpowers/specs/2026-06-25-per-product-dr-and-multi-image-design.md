# Per-Product DR Numbers and Multi-Image Products

## Problem

Two gaps in the restock and product workflows:

1. **DR number is batch-level, not item-level.** The "Restock / Add Stock" modal collects a single DR (Delivery Receipt) number for the whole batch of products being restocked. In practice, different products in the same restock batch can arrive under different DR numbers, so staff need to record a DR per product line, not once per batch.
2. **Products support only one image.** `Product` has a single `productImage` column. Staff want to attach 3-4 reference photos per product (e.g., different angles) so an item can be visually confirmed at sale time.

## Current State

- **rmr** (`com.inventory.vehicle.product`): `Product` has `productImage` (BYTEA) + `productImageType` columns. The restock modal (`ProductListController.openRestockModal`) has one `drNumberField` shared across all rows; `RestockNewProductsCommand.drNumber` is batch-level. `ProductService.restockNewProducts()` validates the DR once and stamps every `StockMovement.referenceId` with that single value. The existing "DR Restock" view (`InventoryController`) groups `StockMovement` rows by `referenceId`, independent of how many distinct DRs exist.
- **inventory-server** (`com.inventory.vehicle.server.product`): `Product` has no image columns at all (this module never picked up the V9 migration's image/restock-date columns) and `ProductController` has no restock endpoint — only create/update/deactivate.

## Scope

Both modules (`rmr` and `inventory-server`) are brought to parity:
- Per-product DR number on restock, in both the desktop UI and a new server restock endpoint.
- Up to 4 images per product, in both modules' schema, entities, and DTOs.

## Design

### 1. DR Number: Batch → Per-Product

**rmr (desktop client, direct DB):**

- Remove the batch-level `drNumberField` from `ProductListController.openRestockModal()`.
- Add a DR number field to `openRestockProductModal()` (the per-product sub-dialog), alongside Quantity and Default Price.
- `RestockProductRow` gains a `drNumber` field; validated as required, non-blank, same as today's batch-level check.
- `CreateProductCommand` gains a `drNumber` field.
- `RestockNewProductsCommand` drops `drNumber` (the restock date remains batch-level — it's a single calendar event — but DR becomes per-product since paperwork can differ per item).
- The restock table (`createRestockTable`) gets a new "DR Number" column so each row's DR is visible before submitting.
- `ProductService.restockNewProducts()`:
  - Removes the single `command.drNumber()` validation.
  - Validates each item's `drNumber` is non-blank when building `StockMovement`.
  - Sets `StockMovement.reason` / `referenceId` from that item's own DR number, not a shared batch value.
  - Audit log message changes from "...from DR X" (singular) to a summary that no longer references one shared DR (e.g., "Created and restocked N new product(s)").
- No changes needed in `InventoryController`'s DR grouping view — it already groups by `referenceId`, so a batch with mixed DRs naturally produces multiple groups.

**inventory-server (REST API):**

- Add `POST /api/products/restock`, `ADMIN`-only, mirroring the rmr flow:
  - Request body: list of items, each with either an existing `productId` + `quantity` + `drNumber`, or new-product fields (`productName`, `brand`, `vehicleType`, `modelCode`, `unitPrice`) + `quantity` + `drNumber`.
  - For each item: create or update the `Product`, increase `currentStock`, write a `StockMovement` with `referenceId = drNumber`, `movementType = RESTOCK`.
  - Validates each item's `drNumber` is non-blank (same rule as rmr).
  - Records one `AuditService` entry summarizing the batch.
  - Returns the list of affected `ProductDto`.

### 2. Multiple Product Images (up to 4)

**Schema (both modules, new Flyway migration):**

- New table `product_images`:
  ```sql
  CREATE TABLE product_images (
      id BIGSERIAL PRIMARY KEY,
      product_id BIGINT NOT NULL REFERENCES products(id),
      image_data BYTEA NOT NULL,
      image_type VARCHAR(80),
      sort_order INTEGER NOT NULL DEFAULT 0,
      created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
  );
  CREATE INDEX idx_product_images_product_id ON product_images(product_id);
  ```
- In **rmr's** migration only: migrate any existing `products.product_image` row (non-null) into `product_images` as `sort_order = 0`, then `ALTER TABLE products DROP COLUMN product_image, DROP COLUMN product_image_type`.
- **inventory-server** never had these columns, so its migration only creates `product_images` — no data migration or column drop needed.
- The 4-image cap is enforced in application code (UI prevents adding a 5th; server validates list size ≤ 4 on create/update), not a DB constraint — keeps the schema simple and avoids triggers.

**rmr entity/UI:**

- `Product` (domain) gets `@OneToMany(mappedBy = "product", cascade = ALL, orphanRemoval = true) List<ProductImage> images`, ordered by `sort_order`. Drop `productImage`/`productImageType` fields and accessors.
- New `ProductImage` entity (`id`, `product` (`@ManyToOne`), `imageData`, `imageType`, `sortOrder`).
- Add/Edit Product modal: "Choose Photo" becomes a small multi-image picker — thumbnail row with an "Add Photo" button (disabled at 4) and a remove (✕) affordance per thumbnail. No reordering (YAGNI).
- Restock-new-product sub-modal (`openRestockProductModal`): same multi-image picker, replacing the current single `photoPreview`/`choosePhotoButton`.
- `RestockProductRow` carries `List<byte[]> productImages` / `List<String> productImageTypes` (parallel lists, capped at 4) instead of single fields.
- Product list/detail view: replace the single static `productImageView` with a thumbnail strip (small `ImageView`s in an `HBox`) or a simple prev/next single-image viewer with index indicator — thumbnail strip is simpler and avoids extra navigation controls; **recommended**.
- Sale/cart screen (`EmployeeSalesController`'s table, which already renders one `ImageView` per row): extend that cell to show the same thumbnail strip, read-only — lets the teller visually confirm the item before completing a sale. No upload capability there.

**inventory-server entity/API:**

- `Product` entity gets the same `@OneToMany List<ProductImage> images` relation; add a new `ProductImage` entity matching the table above.
- `ProductRequest`: add `List<ProductImageRequest> images` (each with base64 `imageData` + `imageType`), validated `@Size(max = 4)`.
- `ProductDto`: add `List<ProductImageDto> images` (id, base64 `imageData`, `imageType`, `sortOrder`).
- `ProductController.create()` / `update()`: persist the image list (replace-all-on-update semantics — simplest, matches how `ProductRequest` already replaces brand/vehicleType/etc. on update).

## Out of Scope

- Image reordering/drag-and-drop.
- Editing/removing a DR number after a restock is submitted (existing DR-group edit dialog in `InventoryController` already supports per-line edits via `UpdateDrRestockLineCommand` — no change needed there since it already operates per-`StockMovement`, i.e., already per-product).
- Enforcing the 4-image cap at the database layer (CHECK constraint or trigger).
- Compressing/resizing uploaded images (existing single-image flow doesn't do this either; not introducing it now).

## Testing Notes

- `ProductService.restockNewProducts()`: unit test that two items with different DR numbers in one batch produce two `StockMovement` rows with distinct `referenceId`s, and that a blank DR on any item rejects the whole batch.
- `inventory-server` new restock endpoint: integration test for ADMIN-only access, stock increase, and `StockMovement` creation per item.
- Image cap: test that submitting a 5th image (UI and server `ProductRequest` validation) is rejected.
- Migration: verify rmr's existing single-image products surface correctly as a 1-item `images` list after migration.
