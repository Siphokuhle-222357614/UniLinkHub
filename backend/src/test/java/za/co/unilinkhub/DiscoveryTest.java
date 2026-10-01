package za.co.unilinkhub;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Finding things: campuses, paged search, category counts, photo galleries and seller trust signals. */
class DiscoveryTest extends ApiTestSupport {

    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n', 0, 0, 0, 0};

    private String upload(String token) throws Exception {
        String body = mockMvc.perform(multipart("/api/images").file(new MockMultipartFile("file", "p.png", "image/png", PNG))
                        .header("Authorization", token))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return json(body).get("url").asText();
    }

    @Test
    void newBusinessesMustSayWhichCampusTheyreOn() throws Exception {
        String token = student();
        mockMvc.perform(post("/api/users/me/become-seller").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"acceptedRules\":true}"));
        mockMvc.perform(post("/api/businesses").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"businessName\":\"Nowhere Shop\",\"description\":\"Things\",\"category\":\"Other\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("choose the CPUT campus")));
        mockMvc.perform(post("/api/businesses").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"businessName\":\"Nowhere Shop\",\"description\":\"Things\",\"category\":\"Other\",\"campus\":\"Atlantis\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("isn't a CPUT campus")));
        mockMvc.perform(get("/api/campuses")).andExpect(jsonPath("$[?(@.key=='DISTRICT_SIX')].label").value(org.hamcrest.Matchers.hasItem("District Six")));
    }

    @Test
    void browseCanFilterByCampusAndShowsWhereToCollect() throws Exception {
        String[] mowbray = seller("MOWBRAY");
        String tag = "campustest" + unique();
        product(mowbray[0], mowbray[1], "Mowbray muffins " + tag, 5);
        String[] wellington = seller("WELLINGTON");
        product(wellington[0], wellington[1], "Wellington waffles " + tag, 5);

        mockMvc.perform(get("/api/listings/search").param("keyword", tag).param("campus", "MOWBRAY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.items[0].campusLabel").value("Mowbray"))
                .andExpect(jsonPath("$.items[0].pickupLocation").value("Res block C"))
                .andExpect(jsonPath("$.items[0].businessName").isNotEmpty());
    }

    @Test
    void searchIsPagedAndCountedByTheDatabase() throws Exception {
        String[] seller = seller();
        String tag = "pagetest" + unique();
        for (int i = 0; i < 5; i++) {
            product(seller[0], seller[1], "Item " + i + " " + tag, 3);
        }
        mockMvc.perform(get("/api/listings/search").param("keyword", tag).param("size", "2").param("page", "0"))
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.totalItems").value(5))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.hasNext").value(true));
        mockMvc.perform(get("/api/listings/search").param("keyword", tag).param("size", "2").param("page", "2"))
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.hasNext").value(false));
        mockMvc.perform(get("/api/listings/search").param("type", "SERVICE").param("keyword", tag))
                .andExpect(jsonPath("$.totalItems").value(0));
        mockMvc.perform(get("/api/listings/category-counts"))
                .andExpect(jsonPath("$.Food", greaterThanOrEqualTo(5)));
        mockMvc.perform(get("/api/listings/search").param("type", "bananas"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Product")));
    }

    @Test
    void listingsCanHaveAPhotoGallery() throws Exception {
        String[] seller = seller();
        String a = upload(seller[0]);
        String b = upload(seller[0]);
        String created = mockMvc.perform(post("/api/listings/products").header("Authorization", seller[0]).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"businessId\":\"%s\",\"name\":\"Gallery mug\",\"description\":\"Two angles\",\"category\":\"Printing\",\"price\":50,\"stockQuantity\":2,\"imageUrls\":[\"%s\",\"%s\"]}"
                                .formatted(seller[1], a, b)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.imageUrls", hasSize(2)))
                .andExpect(jsonPath("$.imageUrl").value(a)) // the first photo is the cover
                .andReturn().getResponse().getContentAsString();
        String id = json(created).get("id").asText();

        // Reorder: the new first photo becomes the cover.
        mockMvc.perform(patch("/api/listings/" + id).header("Authorization", seller[0]).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"imageUrls\":[\"%s\",\"%s\"]}".formatted(b, a)))
                .andExpect(jsonPath("$.imageUrl").value(b));

        StringBuilder seven = new StringBuilder();
        for (int i = 0; i < 7; i++) {
            seven.append(i == 0 ? "" : ",").append('"').append(upload(seller[0])).append('"');
        }
        mockMvc.perform(patch("/api/listings/" + id).header("Authorization", seller[0]).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"imageUrls\":[" + seven + "]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("up to 6 photos")));
    }

    @Test
    void trustSignalsReflectRealReviewsAndCompletedOrders() throws Exception {
        String[] seller = seller();
        String listingId = product(seller[0], seller[1], "Trusty tea", 5);
        String buyer = student();
        String orderJson = mockMvc.perform(post("/api/orders").header("Authorization", buyer).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"listingId\":\"%s\",\"quantity\":1}],\"fulfilmentMethod\":\"PICKUP\"}".formatted(listingId)))
                .andReturn().getResponse().getContentAsString();
        String orderId = json(orderJson).get(0).get("id").asText();
        String code = json(orderJson).get(0).get("pickupCode").asText();
        mockMvc.perform(post("/api/orders/" + orderId + "/confirm").header("Authorization", seller[0]));
        mockMvc.perform(post("/api/orders/" + orderId + "/ready").header("Authorization", seller[0]));
        mockMvc.perform(post("/api/orders/" + orderId + "/complete").header("Authorization", seller[0])
                .contentType(MediaType.APPLICATION_JSON).content("{\"pickupCode\":\"" + code + "\"}")).andExpect(status().isOk());
        mockMvc.perform(post("/api/businesses/" + seller[1] + "/reviews").header("Authorization", buyer)
                .contentType(MediaType.APPLICATION_JSON).content("{\"rating\":4,\"comment\":\"Lovely\"}")).andExpect(status().isOk());

        mockMvc.perform(get("/api/businesses/" + seller[1] + "/trust"))
                .andExpect(jsonPath("$.rating").value(4.0))
                .andExpect(jsonPath("$.reviewCount").value(1))
                .andExpect(jsonPath("$.completedOrders").value(1));
        // Listing pages carry the same signals for the card.
        mockMvc.perform(get("/api/listings/search").param("keyword", "Trusty tea"))
                .andExpect(jsonPath("$.items[*].seller.reviewCount", everyItem(is(1))));
    }
}
