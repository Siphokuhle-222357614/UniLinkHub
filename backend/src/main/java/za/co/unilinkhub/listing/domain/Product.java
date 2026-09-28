package za.co.unilinkhub.listing.domain;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@DiscriminatorValue("PRODUCT")
@Getter
@NoArgsConstructor
public class Product extends Listing {

    private Integer stockQuantity;
    private String imageUrl;
    private Integer lowStockThreshold;

    private Product(UUID businessId, String name, String description, String category,
                     BigDecimal price, Integer stockQuantity, String imageUrl) {
        super(businessId, name, description, category, price);
        this.stockQuantity = stockQuantity;
        this.imageUrl = imageUrl;
    }

    public static Product create(UUID businessId, String name, String description, String category,
                                  BigDecimal price, Integer stockQuantity, String imageUrl) {
        return new Product(businessId, name, description, category, price, stockQuantity, imageUrl);
    }

    public void updateStock(int quantity) {
        this.stockQuantity = quantity;
        if (quantity <= 0) {
            markSoldOut();
        } else if (getStatus() == ListingStatus.SOLD_OUT) {
            reactivate();
        }
    }

    public void updateImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public void updateLowStockThreshold(Integer lowStockThreshold) {
        this.lowStockThreshold = lowStockThreshold;
    }

    public boolean isLowStock() {
        return lowStockThreshold != null && stockQuantity != null
                && stockQuantity > 0 && stockQuantity <= lowStockThreshold;
    }
}
