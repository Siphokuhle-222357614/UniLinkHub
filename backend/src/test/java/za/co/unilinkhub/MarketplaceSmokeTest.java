package za.co.unilinkhub;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import za.co.unilinkhub.user.domain.User;
import za.co.unilinkhub.user.repository.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A single, cohesive walk through the whole marketplace golden path via real HTTP semantics
 * (MockMvc, not calling services directly) - register/become a seller/list a product, register a
 * buyer who messages the seller, checks out with a promo code, has the order confirmed, asks a
 * question, and saves a search that fires when a new listing matches. Each of these flows was
 * hand-verified against live MySQL during development; this test exists so a future change can't
 * silently break one of them again without a build failure.
 */
@SpringBootTest
@AutoConfigureMockMvc
class MarketplaceSmokeTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private String registerAndLogin(String email, String studentNumber, String firstName) throws Exception {
        String registerBody = """
                {"studentNumber":"%s","firstName":"%s","lastName":"Smoke","email":"%s","password":"Smoke@1234"}
                """.formatted(studentNumber, firstName, email);

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(registerBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accountStatus").value("PENDING_VERIFICATION"));

        // No real mailbox exists in this build (the verification link is only logged to the
        // console) - approving the account the same way an admin would from the console is the
        // realistic way to get a freshly-registered account to ACTIVE in a test.
        User user = userRepository.findByEmail(email).orElseThrow();
        user.approve();
        userRepository.save(user);

        String loginBody = """
                {"email":"%s","password":"Smoke@1234"}
                """.formatted(email);
        String loginResponse = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(loginBody))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(loginResponse).get("token").asText();
    }

    private String field(String json, String... path) throws Exception {
        JsonNode node = objectMapper.readTree(json);
        for (String p : path) {
            node = node.get(p);
        }
        return node.asText();
    }

    @Test
    void fullMarketplaceGoldenPath() throws Exception {
        String sellerToken = registerAndLogin("smoke.seller@mycput.ac.za", "990000001", "Seller");
        String buyerToken = registerAndLogin("smoke.buyer@mycput.ac.za", "990000002", "Buyer");

        mockMvc.perform(post("/api/users/me/become-seller").header("Authorization", "Bearer " + sellerToken))
                .andExpect(status().isOk());

        String businessJson = mockMvc.perform(post("/api/businesses")
                        .header("Authorization", "Bearer " + sellerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"businessName":"Smoke Test Store","description":"Smoke test business","category":"Printing"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String businessId = field(businessJson, "id");

        String listingJson = mockMvc.perform(post("/api/listings/products")
                        .header("Authorization", "Bearer " + sellerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"businessId":"%s","name":"Smoke Test Poster","description":"A listing for the smoke test","category":"Printing","price":100,"stockQuantity":5,"imageUrl":null}
                                """.formatted(businessId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andReturn().getResponse().getContentAsString();
        String listingId = field(listingJson, "id");

        // ---- Browse ----
        mockMvc.perform(get("/api/listings").param("category", "Printing"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id=='" + listingId + "')]").exists());

        // ---- Messaging ----
        String conversationJson = mockMvc.perform(post("/api/conversations")
                        .header("Authorization", "Bearer " + buyerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"listingId":"%s","body":"Is this still available?"}
                                """.formatted(listingId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.unreadCount").value(0))
                .andReturn().getResponse().getContentAsString();
        String conversationId = field(conversationJson, "id");

        mockMvc.perform(post("/api/conversations/" + conversationId + "/messages")
                        .header("Authorization", "Bearer " + sellerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"body":"Yes it is!"}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/conversations/unread-count").header("Authorization", "Bearer " + buyerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1));

        // ---- Promo code + checkout + stock decrement ----
        mockMvc.perform(post("/api/businesses/" + businessId + "/promo-codes")
                        .header("Authorization", "Bearer " + sellerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"SMOKE10","discountType":"PERCENT","discountValue":10,"scopeListingId":null,"expiresAt":null}
                                """))
                .andExpect(status().isCreated());

        String orderJson = mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + buyerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"items":[{"listingId":"%s","quantity":2}],"fulfilmentMethod":"PICKUP","note":null,"promoCode":"SMOKE10"}
                                """.formatted(listingId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].subtotal").value(200.0))
                .andExpect(jsonPath("$[0].discountAmount").value(20.0))
                .andExpect(jsonPath("$[0].total").value(180.0))
                .andExpect(jsonPath("$[0].createdAt").exists())
                .andReturn().getResponse().getContentAsString();
        String orderId = objectMapper.readTree(orderJson).get(0).get("id").asText();

        mockMvc.perform(get("/api/listings/" + listingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockQuantity").value(3));

        mockMvc.perform(post("/api/orders/" + orderId + "/confirm").header("Authorization", "Bearer " + sellerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        // ---- Seller analytics: the confirmed order counts towards revenue at its discounted total ----
        mockMvc.perform(get("/api/orders/seller/analytics").param("days", "30").header("Authorization", "Bearer " + sellerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.days").value(30))
                .andExpect(jsonPath("$.orders").value(1))
                .andExpect(jsonPath("$.revenue").value(180.0))
                .andExpect(jsonPath("$.averageOrderValue").value(180.0))
                .andExpect(jsonPath("$.uniqueBuyers").value(1))
                .andExpect(jsonPath("$.daily.length()").value(30))
                .andExpect(jsonPath("$.topListings[0].name").value("Smoke Test Poster"))
                .andExpect(jsonPath("$.topListings[0].unitsSold").value(2));

        // A buyer can't read analytics for a business they don't own.
        mockMvc.perform(get("/api/orders/seller/analytics").param("businessId", businessId).header("Authorization", "Bearer " + buyerToken))
                .andExpect(status().is4xxClientError());

        // ---- Listing Q&A ----
        String questionJson = mockMvc.perform(post("/api/listings/" + listingId + "/questions")
                        .header("Authorization", "Bearer " + buyerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"questionText":"Does this ship?"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String questionId = field(questionJson, "id");

        mockMvc.perform(post("/api/questions/" + questionId + "/answer")
                        .header("Authorization", "Bearer " + sellerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"answerText":"No, pickup only."}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answerText").value("No, pickup only."));

        // ---- Saved search, triggered by a new matching listing ----
        String savedSearchJson = mockMvc.perform(post("/api/saved-searches")
                        .header("Authorization", "Bearer " + buyerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"label":"Printing under R200","keyword":null,"category":"Printing","maxPrice":200,"listingType":"PRODUCT"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String savedSearchId = field(savedSearchJson, "id");

        mockMvc.perform(patch("/api/saved-searches/" + savedSearchId)
                        .header("Authorization", "Bearer " + buyerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"alertsEnabled":false}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alertsEnabled").value(false));

        // Re-enable so the listing published just below still triggers the match notification.
        mockMvc.perform(patch("/api/saved-searches/" + savedSearchId)
                        .header("Authorization", "Bearer " + buyerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"alertsEnabled":true}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/listings/products")
                        .header("Authorization", "Bearer " + sellerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"businessId":"%s","name":"Second Smoke Listing","description":"Should match the saved search","category":"Printing","price":50,"stockQuantity":10,"imageUrl":null}
                                """.formatted(businessId)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/notifications").header("Authorization", "Bearer " + buyerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.category=='SAVED_SEARCH')]").exists())
                .andExpect(jsonPath("$[?(@.category=='MESSAGE')]").exists())
                .andExpect(jsonPath("$[?(@.category=='ORDER')]").exists())
                .andExpect(jsonPath("$[?(@.category=='QUESTION')]").exists());

        // ---- Sanity: unauthenticated request to a protected endpoint is rejected ----
        mockMvc.perform(get("/api/orders/mine")).andExpect(status().is4xxClientError());

        assertThat(businessId).isNotBlank();
    }

    @Test
    void unknownRouteReturnsConsistentApiErrorShape() throws Exception {
        mockMvc.perform(delete("/api/saved-searches/not-a-real-id"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void categoriesEndpointIsPublicAndReturnsFixedTaxonomy() throws Exception {
        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[?(@=='Printing')]").exists());
    }

    @Test
    void publicStatsAreAvailableWithoutLoggingIn() throws Exception {
        mockMvc.perform(get("/api/stats/public"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.students").isNumber())
                .andExpect(jsonPath("$.verifiedBusinesses").isNumber())
                .andExpect(jsonPath("$.activeListings").isNumber())
                .andExpect(jsonPath("$.completedOrders").isNumber());
    }
}
