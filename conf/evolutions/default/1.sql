-- Version 1: Create products table
# --- !Ups
CREATE TABLE products (
                          "ID" BIGSERIAL PRIMARY KEY,
                          "NAME" VARCHAR(255) NOT NULL,
                          "DESCRIPTION" TEXT,
                          "PRICE" BIGINT NOT NULL,
                          "CATEGORY" VARCHAR(100),
                          "IMAGE_KEY" VARCHAR(255),
                          "CUSTOMIZABLE" BOOLEAN,
                          "CREATED_AT" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                          "UPDATED_AT" TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

# --- !Downs
DROP TABLE products;