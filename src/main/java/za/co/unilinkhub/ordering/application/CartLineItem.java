package za.co.unilinkhub.ordering.application;

import java.util.UUID;

public record CartLineItem(UUID listingId, int quantity) {
}
