package za.co.unilinkhub;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Pickup codes: only the buyer sees the code, and the seller needs it to complete the order. */
class OrderPickupTest extends ApiTestSupport {

    @Test
    void sellerNeedsTheBuyersCodeToCompleteAnOrder() throws Exception {
        String[] seller = seller();
        String listingId = product(seller[0], seller[1], "Vetkoek", 10);
        String buyer = student();

        String orderJson = mockMvc.perform(post("/api/orders").header("Authorization", buyer).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"listingId\":\"%s\",\"quantity\":2}],\"fulfilmentMethod\":\"PICKUP\"}".formatted(listingId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].pickupCode").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        String orderId = json(orderJson).get(0).get("id").asText();
        String code = json(orderJson).get(0).get("pickupCode").asText();

        // The seller's view never contains the code - they must get it from the buyer in person.
        mockMvc.perform(get("/api/orders/seller").header("Authorization", seller[0]))
                .andExpect(jsonPath("$[0].pickupCode").doesNotExist())
                .andExpect(jsonPath("$[0].requiresPickupCode").value(true));

        mockMvc.perform(post("/api/orders/" + orderId + "/confirm").header("Authorization", seller[0])).andExpect(status().isOk());
        mockMvc.perform(post("/api/orders/" + orderId + "/ready").header("Authorization", seller[0])).andExpect(status().isOk());

        String wrong = code.equals("0000") ? "1111" : "0000";
        mockMvc.perform(post("/api/orders/" + orderId + "/complete").header("Authorization", seller[0])
                        .contentType(MediaType.APPLICATION_JSON).content("{\"pickupCode\":\"" + wrong + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("doesn't match")))
                .andExpect(jsonPath("$.message", containsString("4 tries left")));

        mockMvc.perform(post("/api/orders/" + orderId + "/complete").header("Authorization", seller[0])
                        .contentType(MediaType.APPLICATION_JSON).content("{\"pickupCode\":\"" + code + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void fiveWrongCodesPauseCompletion() throws Exception {
        String[] seller = seller();
        String listingId = product(seller[0], seller[1], "Samoosas", 10);
        String orderJson = mockMvc.perform(post("/api/orders").header("Authorization", student()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"listingId\":\"%s\",\"quantity\":1}],\"fulfilmentMethod\":\"PICKUP\"}".formatted(listingId)))
                .andReturn().getResponse().getContentAsString();
        String orderId = json(orderJson).get(0).get("id").asText();
        String code = json(orderJson).get(0).get("pickupCode").asText();
        String wrong = code.equals("0000") ? "1111" : "0000";
        mockMvc.perform(post("/api/orders/" + orderId + "/confirm").header("Authorization", seller[0]));
        mockMvc.perform(post("/api/orders/" + orderId + "/ready").header("Authorization", seller[0]));

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/orders/" + orderId + "/complete").header("Authorization", seller[0])
                            .contentType(MediaType.APPLICATION_JSON).content("{\"pickupCode\":\"" + wrong + "\"}"))
                    .andExpect(status().isBadRequest());
        }
        // Even the right code is refused while paused - otherwise guessing all 10,000 codes would work.
        mockMvc.perform(post("/api/orders/" + orderId + "/complete").header("Authorization", seller[0])
                        .contentType(MediaType.APPLICATION_JSON).content("{\"pickupCode\":\"" + code + "\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.message", containsString("wait 15 minutes")));
    }
}
