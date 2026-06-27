-- =========================================================
-- LUCERNE FINAL IMAGE + SIZE + STOCK REPAIR
-- MySQL 8.0+
--
-- Fixes:
-- 1. Missing final products.
-- 2. Old "Abaya Elegant" rows that hide the correct products.
-- 3. Missing image paths.
-- 4. Missing sizes.
-- 5. Missing inventory rows.
-- 6. Exactly 20 total branch items for every color.
-- 7. Exactly 20 total warehouse items for every color.
-- =========================================================

USE lucerne_demo;

SET @OLD_SQL_SAFE_UPDATES := @@SQL_SAFE_UPDATES;
SET @OLD_FOREIGN_KEY_CHECKS := @@FOREIGN_KEY_CHECKS;

SET SQL_SAFE_UPDATES = 0;
SET FOREIGN_KEY_CHECKS = 0;

INSERT IGNORE INTO categories(CategoryName, IsActive) VALUES
('Blouses', TRUE),
('Pants', TRUE),
('Shoes', TRUE),
('Dresses', TRUE),
('Abayas', TRUE);

-- Hide old legacy rows that were appearing before the correct Elegant Abaya.
UPDATE products
SET IsActive = FALSE
WHERE Name IN (
    'Abaya Elegant Black',
    'Abaya Elegant Gray',
    'Abaya Elegant White'
);

-- Ensure all 30 required product-color rows exist.
INSERT INTO products(
    Name, Category, CategoryID, Price, CostPrice,
    ImagePath, Color, Description, Material,
    CareInstructions, IsActive, CreatedAt
)
SELECT
    'Polo Blouse White',
    'Blouses',
    c.CategoryID,
    35.00,
    18.00,
    'out/images/polo_blouse_white.png',
    'White',
    'Elegant short sleeve polo blouse in white.',
    'Mixed fabric',
    'Hand wash recommended',
    TRUE,
    NOW()
FROM categories c
WHERE c.CategoryName = 'Blouses'
  AND NOT EXISTS (
      SELECT 1
      FROM products p
      WHERE LOWER(TRIM(p.Name)) = LOWER(TRIM('Polo Blouse White'))
  );
INSERT INTO products(
    Name, Category, CategoryID, Price, CostPrice,
    ImagePath, Color, Description, Material,
    CareInstructions, IsActive, CreatedAt
)
SELECT
    'Polo Blouse Black',
    'Blouses',
    c.CategoryID,
    35.00,
    18.00,
    'out/images/polo_blouse_black.png',
    'Black',
    'Elegant short sleeve polo blouse in black.',
    'Mixed fabric',
    'Hand wash recommended',
    TRUE,
    NOW()
FROM categories c
WHERE c.CategoryName = 'Blouses'
  AND NOT EXISTS (
      SELECT 1
      FROM products p
      WHERE LOWER(TRIM(p.Name)) = LOWER(TRIM('Polo Blouse Black'))
  );
INSERT INTO products(
    Name, Category, CategoryID, Price, CostPrice,
    ImagePath, Color, Description, Material,
    CareInstructions, IsActive, CreatedAt
)
SELECT
    'Polo Blouse Red',
    'Blouses',
    c.CategoryID,
    35.00,
    18.00,
    'out/images/polo_blouse_red.png',
    'Red',
    'Elegant short sleeve polo blouse in red.',
    'Mixed fabric',
    'Hand wash recommended',
    TRUE,
    NOW()
FROM categories c
WHERE c.CategoryName = 'Blouses'
  AND NOT EXISTS (
      SELECT 1
      FROM products p
      WHERE LOWER(TRIM(p.Name)) = LOWER(TRIM('Polo Blouse Red'))
  );
INSERT INTO products(
    Name, Category, CategoryID, Price, CostPrice,
    ImagePath, Color, Description, Material,
    CareInstructions, IsActive, CreatedAt
)
SELECT
    'Polo Blouse Brown',
    'Blouses',
    c.CategoryID,
    35.00,
    18.00,
    'out/images/polo_blouse_brown.png',
    'Brown',
    'Elegant short sleeve polo blouse in brown.',
    'Mixed fabric',
    'Hand wash recommended',
    TRUE,
    NOW()
FROM categories c
WHERE c.CategoryName = 'Blouses'
  AND NOT EXISTS (
      SELECT 1
      FROM products p
      WHERE LOWER(TRIM(p.Name)) = LOWER(TRIM('Polo Blouse Brown'))
  );
INSERT INTO products(
    Name, Category, CategoryID, Price, CostPrice,
    ImagePath, Color, Description, Material,
    CareInstructions, IsActive, CreatedAt
)
SELECT
    'Polo Blouse Navy',
    'Blouses',
    c.CategoryID,
    35.00,
    18.00,
    'out/images/polo_blouse_navy.png',
    'Navy',
    'Elegant short sleeve polo blouse in navy.',
    'Mixed fabric',
    'Hand wash recommended',
    TRUE,
    NOW()
FROM categories c
WHERE c.CategoryName = 'Blouses'
  AND NOT EXISTS (
      SELECT 1
      FROM products p
      WHERE LOWER(TRIM(p.Name)) = LOWER(TRIM('Polo Blouse Navy'))
  );
INSERT INTO products(
    Name, Category, CategoryID, Price, CostPrice,
    ImagePath, Color, Description, Material,
    CareInstructions, IsActive, CreatedAt
)
SELECT
    'Wide Leg Jeans Black',
    'Pants',
    c.CategoryID,
    55.00,
    30.00,
    'out/images/wide_leg_jeans_black.png',
    'Black',
    'Wide leg denim jeans in black.',
    'Mixed fabric',
    'Hand wash recommended',
    TRUE,
    NOW()
FROM categories c
WHERE c.CategoryName = 'Pants'
  AND NOT EXISTS (
      SELECT 1
      FROM products p
      WHERE LOWER(TRIM(p.Name)) = LOWER(TRIM('Wide Leg Jeans Black'))
  );
INSERT INTO products(
    Name, Category, CategoryID, Price, CostPrice,
    ImagePath, Color, Description, Material,
    CareInstructions, IsActive, CreatedAt
)
SELECT
    'Wide Leg Jeans Dark Blue',
    'Pants',
    c.CategoryID,
    55.00,
    30.00,
    'out/images/wide_leg_jeans_dark_blue.png',
    'Dark Blue',
    'Wide leg denim jeans in dark blue.',
    'Mixed fabric',
    'Hand wash recommended',
    TRUE,
    NOW()
FROM categories c
WHERE c.CategoryName = 'Pants'
  AND NOT EXISTS (
      SELECT 1
      FROM products p
      WHERE LOWER(TRIM(p.Name)) = LOWER(TRIM('Wide Leg Jeans Dark Blue'))
  );
INSERT INTO products(
    Name, Category, CategoryID, Price, CostPrice,
    ImagePath, Color, Description, Material,
    CareInstructions, IsActive, CreatedAt
)
SELECT
    'Wide Leg Jeans Light Blue',
    'Pants',
    c.CategoryID,
    55.00,
    30.00,
    'out/images/wide_leg_jeans_light_blue.png',
    'Light Blue',
    'Wide leg denim jeans in light blue.',
    'Mixed fabric',
    'Hand wash recommended',
    TRUE,
    NOW()
FROM categories c
WHERE c.CategoryName = 'Pants'
  AND NOT EXISTS (
      SELECT 1
      FROM products p
      WHERE LOWER(TRIM(p.Name)) = LOWER(TRIM('Wide Leg Jeans Light Blue'))
  );
INSERT INTO products(
    Name, Category, CategoryID, Price, CostPrice,
    ImagePath, Color, Description, Material,
    CareInstructions, IsActive, CreatedAt
)
SELECT
    'Wide Leg Jeans White',
    'Pants',
    c.CategoryID,
    55.00,
    30.00,
    'out/images/wide_leg_jeans_white.png',
    'White',
    'Wide leg denim jeans in white.',
    'Mixed fabric',
    'Hand wash recommended',
    TRUE,
    NOW()
FROM categories c
WHERE c.CategoryName = 'Pants'
  AND NOT EXISTS (
      SELECT 1
      FROM products p
      WHERE LOWER(TRIM(p.Name)) = LOWER(TRIM('Wide Leg Jeans White'))
  );
INSERT INTO products(
    Name, Category, CategoryID, Price, CostPrice,
    ImagePath, Color, Description, Material,
    CareInstructions, IsActive, CreatedAt
)
SELECT
    'Pointed Heel Nude',
    'Shoes',
    c.CategoryID,
    45.00,
    25.00,
    'out/images/pointed_heel_nude.png',
    'Nude',
    'Pointed heel shoes in nude.',
    'Mixed fabric',
    'Hand wash recommended',
    TRUE,
    NOW()
FROM categories c
WHERE c.CategoryName = 'Shoes'
  AND NOT EXISTS (
      SELECT 1
      FROM products p
      WHERE LOWER(TRIM(p.Name)) = LOWER(TRIM('Pointed Heel Nude'))
  );
INSERT INTO products(
    Name, Category, CategoryID, Price, CostPrice,
    ImagePath, Color, Description, Material,
    CareInstructions, IsActive, CreatedAt
)
SELECT
    'Pointed Heel Red',
    'Shoes',
    c.CategoryID,
    45.00,
    25.00,
    'out/images/pointed_heel_red_original.png',
    'Red',
    'Pointed heel shoes in red.',
    'Mixed fabric',
    'Hand wash recommended',
    TRUE,
    NOW()
FROM categories c
WHERE c.CategoryName = 'Shoes'
  AND NOT EXISTS (
      SELECT 1
      FROM products p
      WHERE LOWER(TRIM(p.Name)) = LOWER(TRIM('Pointed Heel Red'))
  );
INSERT INTO products(
    Name, Category, CategoryID, Price, CostPrice,
    ImagePath, Color, Description, Material,
    CareInstructions, IsActive, CreatedAt
)
SELECT
    'Pointed Heel White',
    'Shoes',
    c.CategoryID,
    45.00,
    25.00,
    'out/images/pointed_heel_white.png',
    'White',
    'Pointed heel shoes in white.',
    'Mixed fabric',
    'Hand wash recommended',
    TRUE,
    NOW()
FROM categories c
WHERE c.CategoryName = 'Shoes'
  AND NOT EXISTS (
      SELECT 1
      FROM products p
      WHERE LOWER(TRIM(p.Name)) = LOWER(TRIM('Pointed Heel White'))
  );
INSERT INTO products(
    Name, Category, CategoryID, Price, CostPrice,
    ImagePath, Color, Description, Material,
    CareInstructions, IsActive, CreatedAt
)
SELECT
    'Pointed Heel Black',
    'Shoes',
    c.CategoryID,
    45.00,
    25.00,
    'out/images/pointed_heel_black.png',
    'Black',
    'Pointed heel shoes in black.',
    'Mixed fabric',
    'Hand wash recommended',
    TRUE,
    NOW()
FROM categories c
WHERE c.CategoryName = 'Shoes'
  AND NOT EXISTS (
      SELECT 1
      FROM products p
      WHERE LOWER(TRIM(p.Name)) = LOWER(TRIM('Pointed Heel Black'))
  );
INSERT INTO products(
    Name, Category, CategoryID, Price, CostPrice,
    ImagePath, Color, Description, Material,
    CareInstructions, IsActive, CreatedAt
)
SELECT
    'Pointed Heel Burgundy',
    'Shoes',
    c.CategoryID,
    45.00,
    25.00,
    'out/images/pointed_heel_burgundy.png',
    'Burgundy',
    'Pointed heel shoes in burgundy.',
    'Mixed fabric',
    'Hand wash recommended',
    TRUE,
    NOW()
FROM categories c
WHERE c.CategoryName = 'Shoes'
  AND NOT EXISTS (
      SELECT 1
      FROM products p
      WHERE LOWER(TRIM(p.Name)) = LOWER(TRIM('Pointed Heel Burgundy'))
  );
INSERT INTO products(
    Name, Category, CategoryID, Price, CostPrice,
    ImagePath, Color, Description, Material,
    CareInstructions, IsActive, CreatedAt
)
SELECT
    'Formal Dress Black',
    'Dresses',
    c.CategoryID,
    90.00,
    50.00,
    'out/images/formal_dress_black.png',
    'Black',
    'One shoulder formal dress in black.',
    'Mixed fabric',
    'Hand wash recommended',
    TRUE,
    NOW()
FROM categories c
WHERE c.CategoryName = 'Dresses'
  AND NOT EXISTS (
      SELECT 1
      FROM products p
      WHERE LOWER(TRIM(p.Name)) = LOWER(TRIM('Formal Dress Black'))
  );
INSERT INTO products(
    Name, Category, CategoryID, Price, CostPrice,
    ImagePath, Color, Description, Material,
    CareInstructions, IsActive, CreatedAt
)
SELECT
    'Formal Dress Blue',
    'Dresses',
    c.CategoryID,
    90.00,
    50.00,
    'out/images/formal_dress_blue.jpg',
    'Blue',
    'One shoulder formal dress in blue.',
    'Mixed fabric',
    'Hand wash recommended',
    TRUE,
    NOW()
FROM categories c
WHERE c.CategoryName = 'Dresses'
  AND NOT EXISTS (
      SELECT 1
      FROM products p
      WHERE LOWER(TRIM(p.Name)) = LOWER(TRIM('Formal Dress Blue'))
  );
INSERT INTO products(
    Name, Category, CategoryID, Price, CostPrice,
    ImagePath, Color, Description, Material,
    CareInstructions, IsActive, CreatedAt
)
SELECT
    'Formal Dress Red',
    'Dresses',
    c.CategoryID,
    90.00,
    50.00,
    'out/images/formal_dress_red.png',
    'Red',
    'One shoulder formal dress in red.',
    'Mixed fabric',
    'Hand wash recommended',
    TRUE,
    NOW()
FROM categories c
WHERE c.CategoryName = 'Dresses'
  AND NOT EXISTS (
      SELECT 1
      FROM products p
      WHERE LOWER(TRIM(p.Name)) = LOWER(TRIM('Formal Dress Red'))
  );
INSERT INTO products(
    Name, Category, CategoryID, Price, CostPrice,
    ImagePath, Color, Description, Material,
    CareInstructions, IsActive, CreatedAt
)
SELECT
    'Formal Dress White',
    'Dresses',
    c.CategoryID,
    90.00,
    50.00,
    'out/images/formal_dress_white.png',
    'White',
    'One shoulder formal dress in white.',
    'Mixed fabric',
    'Hand wash recommended',
    TRUE,
    NOW()
FROM categories c
WHERE c.CategoryName = 'Dresses'
  AND NOT EXISTS (
      SELECT 1
      FROM products p
      WHERE LOWER(TRIM(p.Name)) = LOWER(TRIM('Formal Dress White'))
  );
INSERT INTO products(
    Name, Category, CategoryID, Price, CostPrice,
    ImagePath, Color, Description, Material,
    CareInstructions, IsActive, CreatedAt
)
SELECT
    'Blazer Dress Black',
    'Dresses',
    c.CategoryID,
    80.00,
    45.00,
    'out/images/blazer_dress_black.jpg',
    'Black',
    'Sleeveless blazer dress in black.',
    'Mixed fabric',
    'Hand wash recommended',
    TRUE,
    NOW()
FROM categories c
WHERE c.CategoryName = 'Dresses'
  AND NOT EXISTS (
      SELECT 1
      FROM products p
      WHERE LOWER(TRIM(p.Name)) = LOWER(TRIM('Blazer Dress Black'))
  );
INSERT INTO products(
    Name, Category, CategoryID, Price, CostPrice,
    ImagePath, Color, Description, Material,
    CareInstructions, IsActive, CreatedAt
)
SELECT
    'Blazer Dress Fuchsia',
    'Dresses',
    c.CategoryID,
    80.00,
    45.00,
    'out/images/blazer_dress_fuchsia.jpg',
    'Fuchsia',
    'Sleeveless blazer dress in fuchsia.',
    'Mixed fabric',
    'Hand wash recommended',
    TRUE,
    NOW()
FROM categories c
WHERE c.CategoryName = 'Dresses'
  AND NOT EXISTS (
      SELECT 1
      FROM products p
      WHERE LOWER(TRIM(p.Name)) = LOWER(TRIM('Blazer Dress Fuchsia'))
  );
INSERT INTO products(
    Name, Category, CategoryID, Price, CostPrice,
    ImagePath, Color, Description, Material,
    CareInstructions, IsActive, CreatedAt
)
SELECT
    'Blazer Dress Red',
    'Dresses',
    c.CategoryID,
    80.00,
    45.00,
    'out/images/blazer_dress_red.jpg',
    'Red',
    'Sleeveless blazer dress in red.',
    'Mixed fabric',
    'Hand wash recommended',
    TRUE,
    NOW()
FROM categories c
WHERE c.CategoryName = 'Dresses'
  AND NOT EXISTS (
      SELECT 1
      FROM products p
      WHERE LOWER(TRIM(p.Name)) = LOWER(TRIM('Blazer Dress Red'))
  );
INSERT INTO products(
    Name, Category, CategoryID, Price, CostPrice,
    ImagePath, Color, Description, Material,
    CareInstructions, IsActive, CreatedAt
)
SELECT
    'Elegant Abaya Black',
    'Abayas',
    c.CategoryID,
    130.00,
    75.00,
    'out/images/abaya_elegant_black.jpeg',
    'Black',
    'Elegant classic abaya in black.',
    'Mixed fabric',
    'Hand wash recommended',
    TRUE,
    NOW()
FROM categories c
WHERE c.CategoryName = 'Abayas'
  AND NOT EXISTS (
      SELECT 1
      FROM products p
      WHERE LOWER(TRIM(p.Name)) = LOWER(TRIM('Elegant Abaya Black'))
  );
INSERT INTO products(
    Name, Category, CategoryID, Price, CostPrice,
    ImagePath, Color, Description, Material,
    CareInstructions, IsActive, CreatedAt
)
SELECT
    'Elegant Abaya Gray',
    'Abayas',
    c.CategoryID,
    130.00,
    75.00,
    'out/images/abaya_elegant_gray.jpg',
    'Gray',
    'Elegant classic abaya in gray.',
    'Mixed fabric',
    'Hand wash recommended',
    TRUE,
    NOW()
FROM categories c
WHERE c.CategoryName = 'Abayas'
  AND NOT EXISTS (
      SELECT 1
      FROM products p
      WHERE LOWER(TRIM(p.Name)) = LOWER(TRIM('Elegant Abaya Gray'))
  );
INSERT INTO products(
    Name, Category, CategoryID, Price, CostPrice,
    ImagePath, Color, Description, Material,
    CareInstructions, IsActive, CreatedAt
)
SELECT
    'Elegant Abaya White',
    'Abayas',
    c.CategoryID,
    130.00,
    75.00,
    'out/images/abaya_elegant_white.jpg',
    'White',
    'Elegant classic abaya in white.',
    'Mixed fabric',
    'Hand wash recommended',
    TRUE,
    NOW()
FROM categories c
WHERE c.CategoryName = 'Abayas'
  AND NOT EXISTS (
      SELECT 1
      FROM products p
      WHERE LOWER(TRIM(p.Name)) = LOWER(TRIM('Elegant Abaya White'))
  );
INSERT INTO products(
    Name, Category, CategoryID, Price, CostPrice,
    ImagePath, Color, Description, Material,
    CareInstructions, IsActive, CreatedAt
)
SELECT
    'Embroidered Abaya Beige',
    'Abayas',
    c.CategoryID,
    150.00,
    85.00,
    'out/images/embroidered_abaya_beige.png',
    'Beige',
    'Open embroidered abaya in beige.',
    'Mixed fabric',
    'Hand wash recommended',
    TRUE,
    NOW()
FROM categories c
WHERE c.CategoryName = 'Abayas'
  AND NOT EXISTS (
      SELECT 1
      FROM products p
      WHERE LOWER(TRIM(p.Name)) = LOWER(TRIM('Embroidered Abaya Beige'))
  );
INSERT INTO products(
    Name, Category, CategoryID, Price, CostPrice,
    ImagePath, Color, Description, Material,
    CareInstructions, IsActive, CreatedAt
)
SELECT
    'Embroidered Abaya Black',
    'Abayas',
    c.CategoryID,
    150.00,
    85.00,
    'out/images/embroidered_abaya_black.png',
    'Black',
    'Open embroidered abaya in black.',
    'Mixed fabric',
    'Hand wash recommended',
    TRUE,
    NOW()
FROM categories c
WHERE c.CategoryName = 'Abayas'
  AND NOT EXISTS (
      SELECT 1
      FROM products p
      WHERE LOWER(TRIM(p.Name)) = LOWER(TRIM('Embroidered Abaya Black'))
  );
INSERT INTO products(
    Name, Category, CategoryID, Price, CostPrice,
    ImagePath, Color, Description, Material,
    CareInstructions, IsActive, CreatedAt
)
SELECT
    'Embroidered Abaya Brown',
    'Abayas',
    c.CategoryID,
    150.00,
    85.00,
    'out/images/embroidered_abaya_brown.png',
    'Brown',
    'Open embroidered abaya in brown.',
    'Mixed fabric',
    'Hand wash recommended',
    TRUE,
    NOW()
FROM categories c
WHERE c.CategoryName = 'Abayas'
  AND NOT EXISTS (
      SELECT 1
      FROM products p
      WHERE LOWER(TRIM(p.Name)) = LOWER(TRIM('Embroidered Abaya Brown'))
  );
INSERT INTO products(
    Name, Category, CategoryID, Price, CostPrice,
    ImagePath, Color, Description, Material,
    CareInstructions, IsActive, CreatedAt
)
SELECT
    'Embroidered Abaya Gray',
    'Abayas',
    c.CategoryID,
    150.00,
    85.00,
    'out/images/embroidered_abaya_gray.png',
    'Gray',
    'Open embroidered abaya in gray.',
    'Mixed fabric',
    'Hand wash recommended',
    TRUE,
    NOW()
FROM categories c
WHERE c.CategoryName = 'Abayas'
  AND NOT EXISTS (
      SELECT 1
      FROM products p
      WHERE LOWER(TRIM(p.Name)) = LOWER(TRIM('Embroidered Abaya Gray'))
  );
INSERT INTO products(
    Name, Category, CategoryID, Price, CostPrice,
    ImagePath, Color, Description, Material,
    CareInstructions, IsActive, CreatedAt
)
SELECT
    'Embroidered Abaya Pink',
    'Abayas',
    c.CategoryID,
    150.00,
    85.00,
    'out/images/embroidered_abaya_mauve.png',
    'Pink',
    'Open embroidered abaya in dusty pink.',
    'Mixed fabric',
    'Hand wash recommended',
    TRUE,
    NOW()
FROM categories c
WHERE c.CategoryName = 'Abayas'
  AND NOT EXISTS (
      SELECT 1
      FROM products p
      WHERE LOWER(TRIM(p.Name)) = LOWER(TRIM('Embroidered Abaya Pink'))
  );
INSERT INTO products(
    Name, Category, CategoryID, Price, CostPrice,
    ImagePath, Color, Description, Material,
    CareInstructions, IsActive, CreatedAt
)
SELECT
    'Embroidered Abaya Olive',
    'Abayas',
    c.CategoryID,
    150.00,
    85.00,
    'out/images/embroidered_abaya_olive.png',
    'Olive',
    'Open embroidered abaya in olive.',
    'Mixed fabric',
    'Hand wash recommended',
    TRUE,
    NOW()
FROM categories c
WHERE c.CategoryName = 'Abayas'
  AND NOT EXISTS (
      SELECT 1
      FROM products p
      WHERE LOWER(TRIM(p.Name)) = LOWER(TRIM('Embroidered Abaya Olive'))
  );

-- Correct names, prices, colors and image paths.
UPDATE products p
JOIN categories c
  ON c.CategoryName = 'Blouses'
SET
    p.Category = 'Blouses',
    p.CategoryID = c.CategoryID,
    p.Price = 35.00,
    p.CostPrice = 18.00,
    p.ImagePath = 'out/images/polo_blouse_white.png',
    p.Color = 'White',
    p.Description = 'Elegant short sleeve polo blouse in white.',
    p.Material = 'Mixed fabric',
    p.CareInstructions = 'Hand wash recommended',
    p.IsActive = TRUE
WHERE LOWER(TRIM(p.Name)) = LOWER(TRIM('Polo Blouse White'));
UPDATE products p
JOIN categories c
  ON c.CategoryName = 'Blouses'
SET
    p.Category = 'Blouses',
    p.CategoryID = c.CategoryID,
    p.Price = 35.00,
    p.CostPrice = 18.00,
    p.ImagePath = 'out/images/polo_blouse_black.png',
    p.Color = 'Black',
    p.Description = 'Elegant short sleeve polo blouse in black.',
    p.Material = 'Mixed fabric',
    p.CareInstructions = 'Hand wash recommended',
    p.IsActive = TRUE
WHERE LOWER(TRIM(p.Name)) = LOWER(TRIM('Polo Blouse Black'));
UPDATE products p
JOIN categories c
  ON c.CategoryName = 'Blouses'
SET
    p.Category = 'Blouses',
    p.CategoryID = c.CategoryID,
    p.Price = 35.00,
    p.CostPrice = 18.00,
    p.ImagePath = 'out/images/polo_blouse_red.png',
    p.Color = 'Red',
    p.Description = 'Elegant short sleeve polo blouse in red.',
    p.Material = 'Mixed fabric',
    p.CareInstructions = 'Hand wash recommended',
    p.IsActive = TRUE
WHERE LOWER(TRIM(p.Name)) = LOWER(TRIM('Polo Blouse Red'));
UPDATE products p
JOIN categories c
  ON c.CategoryName = 'Blouses'
SET
    p.Category = 'Blouses',
    p.CategoryID = c.CategoryID,
    p.Price = 35.00,
    p.CostPrice = 18.00,
    p.ImagePath = 'out/images/polo_blouse_brown.png',
    p.Color = 'Brown',
    p.Description = 'Elegant short sleeve polo blouse in brown.',
    p.Material = 'Mixed fabric',
    p.CareInstructions = 'Hand wash recommended',
    p.IsActive = TRUE
WHERE LOWER(TRIM(p.Name)) = LOWER(TRIM('Polo Blouse Brown'));
UPDATE products p
JOIN categories c
  ON c.CategoryName = 'Blouses'
SET
    p.Category = 'Blouses',
    p.CategoryID = c.CategoryID,
    p.Price = 35.00,
    p.CostPrice = 18.00,
    p.ImagePath = 'out/images/polo_blouse_navy.png',
    p.Color = 'Navy',
    p.Description = 'Elegant short sleeve polo blouse in navy.',
    p.Material = 'Mixed fabric',
    p.CareInstructions = 'Hand wash recommended',
    p.IsActive = TRUE
WHERE LOWER(TRIM(p.Name)) = LOWER(TRIM('Polo Blouse Navy'));
UPDATE products p
JOIN categories c
  ON c.CategoryName = 'Pants'
SET
    p.Category = 'Pants',
    p.CategoryID = c.CategoryID,
    p.Price = 55.00,
    p.CostPrice = 30.00,
    p.ImagePath = 'out/images/wide_leg_jeans_black.png',
    p.Color = 'Black',
    p.Description = 'Wide leg denim jeans in black.',
    p.Material = 'Mixed fabric',
    p.CareInstructions = 'Hand wash recommended',
    p.IsActive = TRUE
WHERE LOWER(TRIM(p.Name)) = LOWER(TRIM('Wide Leg Jeans Black'));
UPDATE products p
JOIN categories c
  ON c.CategoryName = 'Pants'
SET
    p.Category = 'Pants',
    p.CategoryID = c.CategoryID,
    p.Price = 55.00,
    p.CostPrice = 30.00,
    p.ImagePath = 'out/images/wide_leg_jeans_dark_blue.png',
    p.Color = 'Dark Blue',
    p.Description = 'Wide leg denim jeans in dark blue.',
    p.Material = 'Mixed fabric',
    p.CareInstructions = 'Hand wash recommended',
    p.IsActive = TRUE
WHERE LOWER(TRIM(p.Name)) = LOWER(TRIM('Wide Leg Jeans Dark Blue'));
UPDATE products p
JOIN categories c
  ON c.CategoryName = 'Pants'
SET
    p.Category = 'Pants',
    p.CategoryID = c.CategoryID,
    p.Price = 55.00,
    p.CostPrice = 30.00,
    p.ImagePath = 'out/images/wide_leg_jeans_light_blue.png',
    p.Color = 'Light Blue',
    p.Description = 'Wide leg denim jeans in light blue.',
    p.Material = 'Mixed fabric',
    p.CareInstructions = 'Hand wash recommended',
    p.IsActive = TRUE
WHERE LOWER(TRIM(p.Name)) = LOWER(TRIM('Wide Leg Jeans Light Blue'));
UPDATE products p
JOIN categories c
  ON c.CategoryName = 'Pants'
SET
    p.Category = 'Pants',
    p.CategoryID = c.CategoryID,
    p.Price = 55.00,
    p.CostPrice = 30.00,
    p.ImagePath = 'out/images/wide_leg_jeans_white.png',
    p.Color = 'White',
    p.Description = 'Wide leg denim jeans in white.',
    p.Material = 'Mixed fabric',
    p.CareInstructions = 'Hand wash recommended',
    p.IsActive = TRUE
WHERE LOWER(TRIM(p.Name)) = LOWER(TRIM('Wide Leg Jeans White'));
UPDATE products p
JOIN categories c
  ON c.CategoryName = 'Shoes'
SET
    p.Category = 'Shoes',
    p.CategoryID = c.CategoryID,
    p.Price = 45.00,
    p.CostPrice = 25.00,
    p.ImagePath = 'out/images/pointed_heel_nude.png',
    p.Color = 'Nude',
    p.Description = 'Pointed heel shoes in nude.',
    p.Material = 'Mixed fabric',
    p.CareInstructions = 'Hand wash recommended',
    p.IsActive = TRUE
WHERE LOWER(TRIM(p.Name)) = LOWER(TRIM('Pointed Heel Nude'));
UPDATE products p
JOIN categories c
  ON c.CategoryName = 'Shoes'
SET
    p.Category = 'Shoes',
    p.CategoryID = c.CategoryID,
    p.Price = 45.00,
    p.CostPrice = 25.00,
    p.ImagePath = 'out/images/pointed_heel_red_original.png',
    p.Color = 'Red',
    p.Description = 'Pointed heel shoes in red.',
    p.Material = 'Mixed fabric',
    p.CareInstructions = 'Hand wash recommended',
    p.IsActive = TRUE
WHERE LOWER(TRIM(p.Name)) = LOWER(TRIM('Pointed Heel Red'));
UPDATE products p
JOIN categories c
  ON c.CategoryName = 'Shoes'
SET
    p.Category = 'Shoes',
    p.CategoryID = c.CategoryID,
    p.Price = 45.00,
    p.CostPrice = 25.00,
    p.ImagePath = 'out/images/pointed_heel_white.png',
    p.Color = 'White',
    p.Description = 'Pointed heel shoes in white.',
    p.Material = 'Mixed fabric',
    p.CareInstructions = 'Hand wash recommended',
    p.IsActive = TRUE
WHERE LOWER(TRIM(p.Name)) = LOWER(TRIM('Pointed Heel White'));
UPDATE products p
JOIN categories c
  ON c.CategoryName = 'Shoes'
SET
    p.Category = 'Shoes',
    p.CategoryID = c.CategoryID,
    p.Price = 45.00,
    p.CostPrice = 25.00,
    p.ImagePath = 'out/images/pointed_heel_black.png',
    p.Color = 'Black',
    p.Description = 'Pointed heel shoes in black.',
    p.Material = 'Mixed fabric',
    p.CareInstructions = 'Hand wash recommended',
    p.IsActive = TRUE
WHERE LOWER(TRIM(p.Name)) = LOWER(TRIM('Pointed Heel Black'));
UPDATE products p
JOIN categories c
  ON c.CategoryName = 'Shoes'
SET
    p.Category = 'Shoes',
    p.CategoryID = c.CategoryID,
    p.Price = 45.00,
    p.CostPrice = 25.00,
    p.ImagePath = 'out/images/pointed_heel_burgundy.png',
    p.Color = 'Burgundy',
    p.Description = 'Pointed heel shoes in burgundy.',
    p.Material = 'Mixed fabric',
    p.CareInstructions = 'Hand wash recommended',
    p.IsActive = TRUE
WHERE LOWER(TRIM(p.Name)) = LOWER(TRIM('Pointed Heel Burgundy'));
UPDATE products p
JOIN categories c
  ON c.CategoryName = 'Dresses'
SET
    p.Category = 'Dresses',
    p.CategoryID = c.CategoryID,
    p.Price = 90.00,
    p.CostPrice = 50.00,
    p.ImagePath = 'out/images/formal_dress_black.png',
    p.Color = 'Black',
    p.Description = 'One shoulder formal dress in black.',
    p.Material = 'Mixed fabric',
    p.CareInstructions = 'Hand wash recommended',
    p.IsActive = TRUE
WHERE LOWER(TRIM(p.Name)) = LOWER(TRIM('Formal Dress Black'));
UPDATE products p
JOIN categories c
  ON c.CategoryName = 'Dresses'
SET
    p.Category = 'Dresses',
    p.CategoryID = c.CategoryID,
    p.Price = 90.00,
    p.CostPrice = 50.00,
    p.ImagePath = 'out/images/formal_dress_blue.jpg',
    p.Color = 'Blue',
    p.Description = 'One shoulder formal dress in blue.',
    p.Material = 'Mixed fabric',
    p.CareInstructions = 'Hand wash recommended',
    p.IsActive = TRUE
WHERE LOWER(TRIM(p.Name)) = LOWER(TRIM('Formal Dress Blue'));
UPDATE products p
JOIN categories c
  ON c.CategoryName = 'Dresses'
SET
    p.Category = 'Dresses',
    p.CategoryID = c.CategoryID,
    p.Price = 90.00,
    p.CostPrice = 50.00,
    p.ImagePath = 'out/images/formal_dress_red.png',
    p.Color = 'Red',
    p.Description = 'One shoulder formal dress in red.',
    p.Material = 'Mixed fabric',
    p.CareInstructions = 'Hand wash recommended',
    p.IsActive = TRUE
WHERE LOWER(TRIM(p.Name)) = LOWER(TRIM('Formal Dress Red'));
UPDATE products p
JOIN categories c
  ON c.CategoryName = 'Dresses'
SET
    p.Category = 'Dresses',
    p.CategoryID = c.CategoryID,
    p.Price = 90.00,
    p.CostPrice = 50.00,
    p.ImagePath = 'out/images/formal_dress_white.png',
    p.Color = 'White',
    p.Description = 'One shoulder formal dress in white.',
    p.Material = 'Mixed fabric',
    p.CareInstructions = 'Hand wash recommended',
    p.IsActive = TRUE
WHERE LOWER(TRIM(p.Name)) = LOWER(TRIM('Formal Dress White'));
UPDATE products p
JOIN categories c
  ON c.CategoryName = 'Dresses'
SET
    p.Category = 'Dresses',
    p.CategoryID = c.CategoryID,
    p.Price = 80.00,
    p.CostPrice = 45.00,
    p.ImagePath = 'out/images/blazer_dress_black.jpg',
    p.Color = 'Black',
    p.Description = 'Sleeveless blazer dress in black.',
    p.Material = 'Mixed fabric',
    p.CareInstructions = 'Hand wash recommended',
    p.IsActive = TRUE
WHERE LOWER(TRIM(p.Name)) = LOWER(TRIM('Blazer Dress Black'));
UPDATE products p
JOIN categories c
  ON c.CategoryName = 'Dresses'
SET
    p.Category = 'Dresses',
    p.CategoryID = c.CategoryID,
    p.Price = 80.00,
    p.CostPrice = 45.00,
    p.ImagePath = 'out/images/blazer_dress_fuchsia.jpg',
    p.Color = 'Fuchsia',
    p.Description = 'Sleeveless blazer dress in fuchsia.',
    p.Material = 'Mixed fabric',
    p.CareInstructions = 'Hand wash recommended',
    p.IsActive = TRUE
WHERE LOWER(TRIM(p.Name)) = LOWER(TRIM('Blazer Dress Fuchsia'));
UPDATE products p
JOIN categories c
  ON c.CategoryName = 'Dresses'
SET
    p.Category = 'Dresses',
    p.CategoryID = c.CategoryID,
    p.Price = 80.00,
    p.CostPrice = 45.00,
    p.ImagePath = 'out/images/blazer_dress_red.jpg',
    p.Color = 'Red',
    p.Description = 'Sleeveless blazer dress in red.',
    p.Material = 'Mixed fabric',
    p.CareInstructions = 'Hand wash recommended',
    p.IsActive = TRUE
WHERE LOWER(TRIM(p.Name)) = LOWER(TRIM('Blazer Dress Red'));
UPDATE products p
JOIN categories c
  ON c.CategoryName = 'Abayas'
SET
    p.Category = 'Abayas',
    p.CategoryID = c.CategoryID,
    p.Price = 130.00,
    p.CostPrice = 75.00,
    p.ImagePath = 'out/images/abaya_elegant_black.jpeg',
    p.Color = 'Black',
    p.Description = 'Elegant classic abaya in black.',
    p.Material = 'Mixed fabric',
    p.CareInstructions = 'Hand wash recommended',
    p.IsActive = TRUE
WHERE LOWER(TRIM(p.Name)) = LOWER(TRIM('Elegant Abaya Black'));
UPDATE products p
JOIN categories c
  ON c.CategoryName = 'Abayas'
SET
    p.Category = 'Abayas',
    p.CategoryID = c.CategoryID,
    p.Price = 130.00,
    p.CostPrice = 75.00,
    p.ImagePath = 'out/images/abaya_elegant_gray.jpg',
    p.Color = 'Gray',
    p.Description = 'Elegant classic abaya in gray.',
    p.Material = 'Mixed fabric',
    p.CareInstructions = 'Hand wash recommended',
    p.IsActive = TRUE
WHERE LOWER(TRIM(p.Name)) = LOWER(TRIM('Elegant Abaya Gray'));
UPDATE products p
JOIN categories c
  ON c.CategoryName = 'Abayas'
SET
    p.Category = 'Abayas',
    p.CategoryID = c.CategoryID,
    p.Price = 130.00,
    p.CostPrice = 75.00,
    p.ImagePath = 'out/images/abaya_elegant_white.jpg',
    p.Color = 'White',
    p.Description = 'Elegant classic abaya in white.',
    p.Material = 'Mixed fabric',
    p.CareInstructions = 'Hand wash recommended',
    p.IsActive = TRUE
WHERE LOWER(TRIM(p.Name)) = LOWER(TRIM('Elegant Abaya White'));
UPDATE products p
JOIN categories c
  ON c.CategoryName = 'Abayas'
SET
    p.Category = 'Abayas',
    p.CategoryID = c.CategoryID,
    p.Price = 150.00,
    p.CostPrice = 85.00,
    p.ImagePath = 'out/images/embroidered_abaya_beige.png',
    p.Color = 'Beige',
    p.Description = 'Open embroidered abaya in beige.',
    p.Material = 'Mixed fabric',
    p.CareInstructions = 'Hand wash recommended',
    p.IsActive = TRUE
WHERE LOWER(TRIM(p.Name)) = LOWER(TRIM('Embroidered Abaya Beige'));
UPDATE products p
JOIN categories c
  ON c.CategoryName = 'Abayas'
SET
    p.Category = 'Abayas',
    p.CategoryID = c.CategoryID,
    p.Price = 150.00,
    p.CostPrice = 85.00,
    p.ImagePath = 'out/images/embroidered_abaya_black.png',
    p.Color = 'Black',
    p.Description = 'Open embroidered abaya in black.',
    p.Material = 'Mixed fabric',
    p.CareInstructions = 'Hand wash recommended',
    p.IsActive = TRUE
WHERE LOWER(TRIM(p.Name)) = LOWER(TRIM('Embroidered Abaya Black'));
UPDATE products p
JOIN categories c
  ON c.CategoryName = 'Abayas'
SET
    p.Category = 'Abayas',
    p.CategoryID = c.CategoryID,
    p.Price = 150.00,
    p.CostPrice = 85.00,
    p.ImagePath = 'out/images/embroidered_abaya_brown.png',
    p.Color = 'Brown',
    p.Description = 'Open embroidered abaya in brown.',
    p.Material = 'Mixed fabric',
    p.CareInstructions = 'Hand wash recommended',
    p.IsActive = TRUE
WHERE LOWER(TRIM(p.Name)) = LOWER(TRIM('Embroidered Abaya Brown'));
UPDATE products p
JOIN categories c
  ON c.CategoryName = 'Abayas'
SET
    p.Category = 'Abayas',
    p.CategoryID = c.CategoryID,
    p.Price = 150.00,
    p.CostPrice = 85.00,
    p.ImagePath = 'out/images/embroidered_abaya_gray.png',
    p.Color = 'Gray',
    p.Description = 'Open embroidered abaya in gray.',
    p.Material = 'Mixed fabric',
    p.CareInstructions = 'Hand wash recommended',
    p.IsActive = TRUE
WHERE LOWER(TRIM(p.Name)) = LOWER(TRIM('Embroidered Abaya Gray'));
UPDATE products p
JOIN categories c
  ON c.CategoryName = 'Abayas'
SET
    p.Category = 'Abayas',
    p.CategoryID = c.CategoryID,
    p.Price = 150.00,
    p.CostPrice = 85.00,
    p.ImagePath = 'out/images/embroidered_abaya_mauve.png',
    p.Color = 'Pink',
    p.Description = 'Open embroidered abaya in dusty pink.',
    p.Material = 'Mixed fabric',
    p.CareInstructions = 'Hand wash recommended',
    p.IsActive = TRUE
WHERE LOWER(TRIM(p.Name)) = LOWER(TRIM('Embroidered Abaya Pink'));
UPDATE products p
JOIN categories c
  ON c.CategoryName = 'Abayas'
SET
    p.Category = 'Abayas',
    p.CategoryID = c.CategoryID,
    p.Price = 150.00,
    p.CostPrice = 85.00,
    p.ImagePath = 'out/images/embroidered_abaya_olive.png',
    p.Color = 'Olive',
    p.Description = 'Open embroidered abaya in olive.',
    p.Material = 'Mixed fabric',
    p.CareInstructions = 'Hand wash recommended',
    p.IsActive = TRUE
WHERE LOWER(TRIM(p.Name)) = LOWER(TRIM('Embroidered Abaya Olive'));

-- Keep only the oldest ProductID active if duplicate rows exist.
UPDATE products duplicate_product
JOIN (
    SELECT
        LOWER(TRIM(Name)) AS NormalizedName,
        MIN(ProductID) AS KeepProductID
    FROM products
    WHERE Name IN (
        'Polo Blouse White',
    'Polo Blouse Black',
    'Polo Blouse Red',
    'Polo Blouse Brown',
    'Polo Blouse Navy',
    'Wide Leg Jeans Black',
    'Wide Leg Jeans Dark Blue',
    'Wide Leg Jeans Light Blue',
    'Wide Leg Jeans White',
    'Pointed Heel Nude',
    'Pointed Heel Red',
    'Pointed Heel White',
    'Pointed Heel Black',
    'Pointed Heel Burgundy',
    'Formal Dress Black',
    'Formal Dress Blue',
    'Formal Dress Red',
    'Formal Dress White',
    'Blazer Dress Black',
    'Blazer Dress Fuchsia',
    'Blazer Dress Red',
    'Elegant Abaya Black',
    'Elegant Abaya Gray',
    'Elegant Abaya White',
    'Embroidered Abaya Beige',
    'Embroidered Abaya Black',
    'Embroidered Abaya Brown',
    'Embroidered Abaya Gray',
    'Embroidered Abaya Pink',
    'Embroidered Abaya Olive'
    )
    GROUP BY LOWER(TRIM(Name))
) keeper
  ON LOWER(TRIM(duplicate_product.Name)) = keeper.NormalizedName
SET duplicate_product.IsActive =
    CASE
        WHEN duplicate_product.ProductID = keeper.KeepProductID
        THEN TRUE
        ELSE FALSE
    END
WHERE duplicate_product.Name IN (
    'Polo Blouse White',
    'Polo Blouse Black',
    'Polo Blouse Red',
    'Polo Blouse Brown',
    'Polo Blouse Navy',
    'Wide Leg Jeans Black',
    'Wide Leg Jeans Dark Blue',
    'Wide Leg Jeans Light Blue',
    'Wide Leg Jeans White',
    'Pointed Heel Nude',
    'Pointed Heel Red',
    'Pointed Heel White',
    'Pointed Heel Black',
    'Pointed Heel Burgundy',
    'Formal Dress Black',
    'Formal Dress Blue',
    'Formal Dress Red',
    'Formal Dress White',
    'Blazer Dress Black',
    'Blazer Dress Fuchsia',
    'Blazer Dress Red',
    'Elegant Abaya Black',
    'Elegant Abaya Gray',
    'Elegant Abaya White',
    'Embroidered Abaya Beige',
    'Embroidered Abaya Black',
    'Embroidered Abaya Brown',
    'Embroidered Abaya Gray',
    'Embroidered Abaya Pink',
    'Embroidered Abaya Olive'
);

DROP TEMPORARY TABLE IF EXISTS tmp_required_product_sizes;

CREATE TEMPORARY TABLE tmp_required_product_sizes (
    ProductName VARCHAR(180) NOT NULL,
    SizeValue VARCHAR(30) NOT NULL,
    PRIMARY KEY(ProductName, SizeValue)
);

INSERT INTO tmp_required_product_sizes(ProductName, SizeValue) VALUES
    ('Polo Blouse White','S'),
    ('Polo Blouse White','M'),
    ('Polo Blouse White','L'),
    ('Polo Blouse White','XL'),
    ('Polo Blouse Black','S'),
    ('Polo Blouse Black','M'),
    ('Polo Blouse Black','L'),
    ('Polo Blouse Black','XL'),
    ('Polo Blouse Red','S'),
    ('Polo Blouse Red','M'),
    ('Polo Blouse Red','L'),
    ('Polo Blouse Red','XL'),
    ('Polo Blouse Brown','S'),
    ('Polo Blouse Brown','M'),
    ('Polo Blouse Brown','L'),
    ('Polo Blouse Brown','XL'),
    ('Polo Blouse Navy','S'),
    ('Polo Blouse Navy','M'),
    ('Polo Blouse Navy','L'),
    ('Polo Blouse Navy','XL'),
    ('Wide Leg Jeans Black','36'),
    ('Wide Leg Jeans Black','38'),
    ('Wide Leg Jeans Black','40'),
    ('Wide Leg Jeans Black','42'),
    ('Wide Leg Jeans Dark Blue','36'),
    ('Wide Leg Jeans Dark Blue','38'),
    ('Wide Leg Jeans Dark Blue','40'),
    ('Wide Leg Jeans Dark Blue','42'),
    ('Wide Leg Jeans Light Blue','36'),
    ('Wide Leg Jeans Light Blue','38'),
    ('Wide Leg Jeans Light Blue','40'),
    ('Wide Leg Jeans Light Blue','42'),
    ('Wide Leg Jeans White','36'),
    ('Wide Leg Jeans White','38'),
    ('Wide Leg Jeans White','40'),
    ('Wide Leg Jeans White','42'),
    ('Pointed Heel Nude','36'),
    ('Pointed Heel Nude','37'),
    ('Pointed Heel Nude','38'),
    ('Pointed Heel Nude','39'),
    ('Pointed Heel Nude','40'),
    ('Pointed Heel Nude','41'),
    ('Pointed Heel Red','36'),
    ('Pointed Heel Red','37'),
    ('Pointed Heel Red','38'),
    ('Pointed Heel Red','39'),
    ('Pointed Heel Red','40'),
    ('Pointed Heel Red','41'),
    ('Pointed Heel White','36'),
    ('Pointed Heel White','37'),
    ('Pointed Heel White','38'),
    ('Pointed Heel White','39'),
    ('Pointed Heel White','40'),
    ('Pointed Heel White','41'),
    ('Pointed Heel Black','36'),
    ('Pointed Heel Black','37'),
    ('Pointed Heel Black','38'),
    ('Pointed Heel Black','39'),
    ('Pointed Heel Black','40'),
    ('Pointed Heel Black','41'),
    ('Pointed Heel Burgundy','36'),
    ('Pointed Heel Burgundy','37'),
    ('Pointed Heel Burgundy','38'),
    ('Pointed Heel Burgundy','39'),
    ('Pointed Heel Burgundy','40'),
    ('Pointed Heel Burgundy','41'),
    ('Formal Dress Black','S'),
    ('Formal Dress Black','M'),
    ('Formal Dress Black','L'),
    ('Formal Dress Black','XL'),
    ('Formal Dress Blue','S'),
    ('Formal Dress Blue','M'),
    ('Formal Dress Blue','L'),
    ('Formal Dress Blue','XL'),
    ('Formal Dress Red','S'),
    ('Formal Dress Red','M'),
    ('Formal Dress Red','L'),
    ('Formal Dress Red','XL'),
    ('Formal Dress White','S'),
    ('Formal Dress White','M'),
    ('Formal Dress White','L'),
    ('Formal Dress White','XL'),
    ('Blazer Dress Black','S'),
    ('Blazer Dress Black','M'),
    ('Blazer Dress Black','L'),
    ('Blazer Dress Black','XL'),
    ('Blazer Dress Fuchsia','S'),
    ('Blazer Dress Fuchsia','M'),
    ('Blazer Dress Fuchsia','L'),
    ('Blazer Dress Fuchsia','XL'),
    ('Blazer Dress Red','S'),
    ('Blazer Dress Red','M'),
    ('Blazer Dress Red','L'),
    ('Blazer Dress Red','XL'),
    ('Elegant Abaya Black','S'),
    ('Elegant Abaya Black','M'),
    ('Elegant Abaya Black','L'),
    ('Elegant Abaya Black','XL'),
    ('Elegant Abaya Gray','S'),
    ('Elegant Abaya Gray','M'),
    ('Elegant Abaya Gray','L'),
    ('Elegant Abaya Gray','XL'),
    ('Elegant Abaya White','S'),
    ('Elegant Abaya White','M'),
    ('Elegant Abaya White','L'),
    ('Elegant Abaya White','XL'),
    ('Embroidered Abaya Beige','S'),
    ('Embroidered Abaya Beige','M'),
    ('Embroidered Abaya Beige','L'),
    ('Embroidered Abaya Beige','XL'),
    ('Embroidered Abaya Black','S'),
    ('Embroidered Abaya Black','M'),
    ('Embroidered Abaya Black','L'),
    ('Embroidered Abaya Black','XL'),
    ('Embroidered Abaya Brown','S'),
    ('Embroidered Abaya Brown','M'),
    ('Embroidered Abaya Brown','L'),
    ('Embroidered Abaya Brown','XL'),
    ('Embroidered Abaya Gray','S'),
    ('Embroidered Abaya Gray','M'),
    ('Embroidered Abaya Gray','L'),
    ('Embroidered Abaya Gray','XL'),
    ('Embroidered Abaya Pink','S'),
    ('Embroidered Abaya Pink','M'),
    ('Embroidered Abaya Pink','L'),
    ('Embroidered Abaya Pink','XL'),
    ('Embroidered Abaya Olive','S'),
    ('Embroidered Abaya Olive','M'),
    ('Embroidered Abaya Olive','L'),
    ('Embroidered Abaya Olive','XL');

-- Add all missing sizes to the active product row.
INSERT INTO product_sizes(ProductID, SizeValue)
SELECT
    p.ProductID,
    required_size.SizeValue
FROM products p
JOIN tmp_required_product_sizes required_size
  ON LOWER(TRIM(required_size.ProductName)) =
     LOWER(TRIM(p.Name))
WHERE p.IsActive = TRUE
  AND NOT EXISTS (
      SELECT 1
      FROM product_sizes existing_size
      WHERE existing_size.ProductID = p.ProductID
        AND LOWER(TRIM(existing_size.SizeValue)) =
            LOWER(TRIM(required_size.SizeValue))
  );

-- Create missing branch inventory rows.
INSERT INTO branch_inventory(
    BranchID, ProductID, SizeID, Quantity, MinQuantity
)
SELECT
    branch_data.BranchID,
    product_size.ProductID,
    product_size.SizeID,
    0,
    2
FROM branches branch_data
JOIN product_sizes product_size
JOIN products product_data
  ON product_data.ProductID = product_size.ProductID
WHERE product_data.IsActive = TRUE
  AND product_data.Name IN (
    'Polo Blouse White',
    'Polo Blouse Black',
    'Polo Blouse Red',
    'Polo Blouse Brown',
    'Polo Blouse Navy',
    'Wide Leg Jeans Black',
    'Wide Leg Jeans Dark Blue',
    'Wide Leg Jeans Light Blue',
    'Wide Leg Jeans White',
    'Pointed Heel Nude',
    'Pointed Heel Red',
    'Pointed Heel White',
    'Pointed Heel Black',
    'Pointed Heel Burgundy',
    'Formal Dress Black',
    'Formal Dress Blue',
    'Formal Dress Red',
    'Formal Dress White',
    'Blazer Dress Black',
    'Blazer Dress Fuchsia',
    'Blazer Dress Red',
    'Elegant Abaya Black',
    'Elegant Abaya Gray',
    'Elegant Abaya White',
    'Embroidered Abaya Beige',
    'Embroidered Abaya Black',
    'Embroidered Abaya Brown',
    'Embroidered Abaya Gray',
    'Embroidered Abaya Pink',
    'Embroidered Abaya Olive'
  )
  AND NOT EXISTS (
      SELECT 1
      FROM branch_inventory existing_inventory
      WHERE existing_inventory.BranchID = branch_data.BranchID
        AND existing_inventory.ProductID = product_size.ProductID
        AND existing_inventory.SizeID = product_size.SizeID
  );

-- Create missing warehouse inventory rows.
INSERT INTO warehouse_inventory(
    WarehouseID, ProductID, SizeID, Quantity
)
SELECT
    warehouse_data.WarehouseID,
    product_size.ProductID,
    product_size.SizeID,
    0
FROM warehouses warehouse_data
JOIN product_sizes product_size
JOIN products product_data
  ON product_data.ProductID = product_size.ProductID
WHERE product_data.IsActive = TRUE
  AND product_data.Name IN (
    'Polo Blouse White',
    'Polo Blouse Black',
    'Polo Blouse Red',
    'Polo Blouse Brown',
    'Polo Blouse Navy',
    'Wide Leg Jeans Black',
    'Wide Leg Jeans Dark Blue',
    'Wide Leg Jeans Light Blue',
    'Wide Leg Jeans White',
    'Pointed Heel Nude',
    'Pointed Heel Red',
    'Pointed Heel White',
    'Pointed Heel Black',
    'Pointed Heel Burgundy',
    'Formal Dress Black',
    'Formal Dress Blue',
    'Formal Dress Red',
    'Formal Dress White',
    'Blazer Dress Black',
    'Blazer Dress Fuchsia',
    'Blazer Dress Red',
    'Elegant Abaya Black',
    'Elegant Abaya Gray',
    'Elegant Abaya White',
    'Embroidered Abaya Beige',
    'Embroidered Abaya Black',
    'Embroidered Abaya Brown',
    'Embroidered Abaya Gray',
    'Embroidered Abaya Pink',
    'Embroidered Abaya Olive'
  )
  AND NOT EXISTS (
      SELECT 1
      FROM warehouse_inventory existing_inventory
      WHERE existing_inventory.WarehouseID =
            warehouse_data.WarehouseID
        AND existing_inventory.ProductID =
            product_size.ProductID
        AND existing_inventory.SizeID =
            product_size.SizeID
  );

-- Distribute exactly 20 items over branch-size rows for each color.
DROP TEMPORARY TABLE IF EXISTS tmp_branch_distribution;

CREATE TEMPORARY TABLE tmp_branch_distribution AS
SELECT
    inventory.BranchID,
    inventory.ProductID,
    inventory.SizeID,
    ROW_NUMBER() OVER (
        PARTITION BY inventory.ProductID
        ORDER BY inventory.BranchID, inventory.SizeID
    ) AS RowNumberInProduct,
    COUNT(*) OVER (
        PARTITION BY inventory.ProductID
    ) AS ProductRowCount
FROM branch_inventory inventory
JOIN products product_data
  ON product_data.ProductID = inventory.ProductID
WHERE product_data.IsActive = TRUE
  AND product_data.Name IN (
    'Polo Blouse White',
    'Polo Blouse Black',
    'Polo Blouse Red',
    'Polo Blouse Brown',
    'Polo Blouse Navy',
    'Wide Leg Jeans Black',
    'Wide Leg Jeans Dark Blue',
    'Wide Leg Jeans Light Blue',
    'Wide Leg Jeans White',
    'Pointed Heel Nude',
    'Pointed Heel Red',
    'Pointed Heel White',
    'Pointed Heel Black',
    'Pointed Heel Burgundy',
    'Formal Dress Black',
    'Formal Dress Blue',
    'Formal Dress Red',
    'Formal Dress White',
    'Blazer Dress Black',
    'Blazer Dress Fuchsia',
    'Blazer Dress Red',
    'Elegant Abaya Black',
    'Elegant Abaya Gray',
    'Elegant Abaya White',
    'Embroidered Abaya Beige',
    'Embroidered Abaya Black',
    'Embroidered Abaya Brown',
    'Embroidered Abaya Gray',
    'Embroidered Abaya Pink',
    'Embroidered Abaya Olive'
  );

UPDATE branch_inventory inventory
JOIN tmp_branch_distribution distribution_data
  ON distribution_data.BranchID = inventory.BranchID
 AND distribution_data.ProductID = inventory.ProductID
 AND distribution_data.SizeID = inventory.SizeID
SET
    inventory.Quantity =
        FLOOR(20 / distribution_data.ProductRowCount)
        +
        CASE
            WHEN distribution_data.RowNumberInProduct <=
                 MOD(20, distribution_data.ProductRowCount)
            THEN 1
            ELSE 0
        END,
    inventory.MinQuantity = 2;

DROP TEMPORARY TABLE IF EXISTS tmp_branch_distribution;

-- Distribute exactly 20 items over warehouse-size rows for each color.
DROP TEMPORARY TABLE IF EXISTS tmp_warehouse_distribution;

CREATE TEMPORARY TABLE tmp_warehouse_distribution AS
SELECT
    inventory.WarehouseID,
    inventory.ProductID,
    inventory.SizeID,
    ROW_NUMBER() OVER (
        PARTITION BY inventory.ProductID
        ORDER BY inventory.WarehouseID, inventory.SizeID
    ) AS RowNumberInProduct,
    COUNT(*) OVER (
        PARTITION BY inventory.ProductID
    ) AS ProductRowCount
FROM warehouse_inventory inventory
JOIN products product_data
  ON product_data.ProductID = inventory.ProductID
WHERE product_data.IsActive = TRUE
  AND product_data.Name IN (
    'Polo Blouse White',
    'Polo Blouse Black',
    'Polo Blouse Red',
    'Polo Blouse Brown',
    'Polo Blouse Navy',
    'Wide Leg Jeans Black',
    'Wide Leg Jeans Dark Blue',
    'Wide Leg Jeans Light Blue',
    'Wide Leg Jeans White',
    'Pointed Heel Nude',
    'Pointed Heel Red',
    'Pointed Heel White',
    'Pointed Heel Black',
    'Pointed Heel Burgundy',
    'Formal Dress Black',
    'Formal Dress Blue',
    'Formal Dress Red',
    'Formal Dress White',
    'Blazer Dress Black',
    'Blazer Dress Fuchsia',
    'Blazer Dress Red',
    'Elegant Abaya Black',
    'Elegant Abaya Gray',
    'Elegant Abaya White',
    'Embroidered Abaya Beige',
    'Embroidered Abaya Black',
    'Embroidered Abaya Brown',
    'Embroidered Abaya Gray',
    'Embroidered Abaya Pink',
    'Embroidered Abaya Olive'
  );

UPDATE warehouse_inventory inventory
JOIN tmp_warehouse_distribution distribution_data
  ON distribution_data.WarehouseID = inventory.WarehouseID
 AND distribution_data.ProductID = inventory.ProductID
 AND distribution_data.SizeID = inventory.SizeID
SET inventory.Quantity =
        FLOOR(20 / distribution_data.ProductRowCount)
        +
        CASE
            WHEN distribution_data.RowNumberInProduct <=
                 MOD(20, distribution_data.ProductRowCount)
            THEN 1
            ELSE 0
        END;

DROP TEMPORARY TABLE IF EXISTS tmp_warehouse_distribution;
DROP TEMPORARY TABLE IF EXISTS tmp_required_product_sizes;

SET FOREIGN_KEY_CHECKS = @OLD_FOREIGN_KEY_CHECKS;
SET SQL_SAFE_UPDATES = @OLD_SQL_SAFE_UPDATES;

-- Final verification.
SELECT
    product_data.ProductID,
    product_data.Name,
    product_data.Color,
    product_data.ImagePath,
    GROUP_CONCAT(
        DISTINCT product_size.SizeValue
        ORDER BY
            CASE UPPER(TRIM(product_size.SizeValue))
                WHEN 'XXS' THEN 1
                WHEN 'XS' THEN 2
                WHEN 'S' THEN 3
                WHEN 'M' THEN 4
                WHEN 'L' THEN 5
                WHEN 'XL' THEN 6
                WHEN 'XXL' THEN 7
                ELSE 8
            END,
            CAST(product_size.SizeValue AS UNSIGNED),
            product_size.SizeValue
        SEPARATOR ', '
    ) AS Sizes,
    COALESCE(branch_stock.TotalQuantity, 0) AS BranchQuantity,
    COALESCE(warehouse_stock.TotalQuantity, 0) AS WarehouseQuantity
FROM products product_data
LEFT JOIN product_sizes product_size
  ON product_size.ProductID = product_data.ProductID
LEFT JOIN (
    SELECT ProductID, SUM(Quantity) AS TotalQuantity
    FROM branch_inventory
    GROUP BY ProductID
) branch_stock
  ON branch_stock.ProductID = product_data.ProductID
LEFT JOIN (
    SELECT ProductID, SUM(Quantity) AS TotalQuantity
    FROM warehouse_inventory
    GROUP BY ProductID
) warehouse_stock
  ON warehouse_stock.ProductID = product_data.ProductID
WHERE product_data.IsActive = TRUE
  AND product_data.Name IN (
    'Polo Blouse White',
    'Polo Blouse Black',
    'Polo Blouse Red',
    'Polo Blouse Brown',
    'Polo Blouse Navy',
    'Wide Leg Jeans Black',
    'Wide Leg Jeans Dark Blue',
    'Wide Leg Jeans Light Blue',
    'Wide Leg Jeans White',
    'Pointed Heel Nude',
    'Pointed Heel Red',
    'Pointed Heel White',
    'Pointed Heel Black',
    'Pointed Heel Burgundy',
    'Formal Dress Black',
    'Formal Dress Blue',
    'Formal Dress Red',
    'Formal Dress White',
    'Blazer Dress Black',
    'Blazer Dress Fuchsia',
    'Blazer Dress Red',
    'Elegant Abaya Black',
    'Elegant Abaya Gray',
    'Elegant Abaya White',
    'Embroidered Abaya Beige',
    'Embroidered Abaya Black',
    'Embroidered Abaya Brown',
    'Embroidered Abaya Gray',
    'Embroidered Abaya Pink',
    'Embroidered Abaya Olive'
  )
GROUP BY
    product_data.ProductID,
    product_data.Name,
    product_data.Color,
    product_data.ImagePath,
    branch_stock.TotalQuantity,
    warehouse_stock.TotalQuantity
ORDER BY product_data.Category, product_data.Name, product_data.Color;

SELECT
    CASE
        WHEN COUNT(*) = 30
        THEN 'SUCCESS: all 30 active products exist'
        ELSE CONCAT(
            'WARNING: active products = ',
            COUNT(*),
            ', expected 30'
        )
    END AS ProductCheck
FROM products
WHERE IsActive = TRUE
  AND Name IN (
    'Polo Blouse White',
    'Polo Blouse Black',
    'Polo Blouse Red',
    'Polo Blouse Brown',
    'Polo Blouse Navy',
    'Wide Leg Jeans Black',
    'Wide Leg Jeans Dark Blue',
    'Wide Leg Jeans Light Blue',
    'Wide Leg Jeans White',
    'Pointed Heel Nude',
    'Pointed Heel Red',
    'Pointed Heel White',
    'Pointed Heel Black',
    'Pointed Heel Burgundy',
    'Formal Dress Black',
    'Formal Dress Blue',
    'Formal Dress Red',
    'Formal Dress White',
    'Blazer Dress Black',
    'Blazer Dress Fuchsia',
    'Blazer Dress Red',
    'Elegant Abaya Black',
    'Elegant Abaya Gray',
    'Elegant Abaya White',
    'Embroidered Abaya Beige',
    'Embroidered Abaya Black',
    'Embroidered Abaya Brown',
    'Embroidered Abaya Gray',
    'Embroidered Abaya Pink',
    'Embroidered Abaya Olive'
  );

SELECT 'IMAGE, SIZE AND STOCK REPAIR COMPLETED' AS Result;
