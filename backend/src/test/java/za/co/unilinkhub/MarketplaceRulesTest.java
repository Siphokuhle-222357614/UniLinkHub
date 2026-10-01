package za.co.unilinkhub;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The marketplace's rules, each checked through real HTTP: who may do what, what may be sold,
 * and - for every rejection - that the response explains why in plain English.
 */
class MarketplaceRulesTest extends ApiTestSupport {

    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n', 0, 0, 0, 0};

    // ---------------------------------------------------------------- sign-up & verification

    @Test
    void onlyCputStudentEmailsCanRegister() throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                        {"studentNumber":"77%s","firstName":"A","lastName":"B","email":"someone@gmail.com","password":"Rules@1234"}
                        """.formatted(unique())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("@mycput.ac.za")));
    }

    @Test
    void studentsVerifyThemselvesByEmailBeforeTheyCanLogIn() throws Exception {
        String email = studentEmail("unverified");
        register(email);

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"Rules@1234\"}".formatted(email)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("EMAIL_NOT_VERIFIED"))
                .andExpect(jsonPath("$.message", containsString("verify your email")));

        String firstToken = userRepository.findByEmail(email).orElseThrow().getVerificationToken();
        mockMvc.perform(post("/api/auth/resend-verification").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\"}".formatted(email)))
                .andExpect(status().isNoContent());
        // Resending replaces the link, so an old email can't be used after a new one was requested.
        mockMvc.perform(get("/api/auth/verify").param("token", firstToken)).andExpect(status().isBadRequest());

        verify(email);
        login(email, "Rules@1234");
    }

    @Test
    void validationErrorsUsePlainEnglish() throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentNumber\":\"\",\"firstName\":\"A\",\"lastName\":\"B\",\"email\":\"x@mycput.ac.za\",\"password\":\"Rules@1234\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Student number is required."));
    }

    @Test
    void repeatedWrongPasswordsLockTheAccountForAWhile() throws Exception {
        String email = studentEmail("guessed");
        register(email);
        verify(email);
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"%s\",\"password\":\"wrong-%d\"}".formatted(email, i)))
                    .andExpect(status().isUnauthorized());
        }
        // Even the right password is refused until the window passes - that's what stops guessing.
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"Rules@1234\"}".formatted(email)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.message", containsString("Too many wrong passwords")))
                .andExpect(jsonPath("$.message", containsString("minute")));
    }

    @Test
    void passwordResetEmailsAreLimitedPerAddress() throws Exception {
        String email = studentEmail("reset");
        register(email);
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/api/auth/forgot-password").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"%s\"}".formatted(email)))
                    .andExpect(status().isNoContent());
        }
        mockMvc.perform(post("/api/auth/forgot-password").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\"}".formatted(email)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.message", containsString("check your inbox")));
    }

    // ---------------------------------------------------------------- who may do what

    @Test
    void loggedOutRequestsAreToldToLogIn() throws Exception {
        mockMvc.perform(get("/api/orders/mine"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", containsString("Please log in")));
    }

    @Test
    void studentsAreToldAdminPagesAreForAdmins() throws Exception {
        mockMvc.perform(get("/api/admin/stats").header("Authorization", student()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", containsString("only for administrators")));
    }

    @Test
    void adminAccountsCantTakePartInTheMarketplace() throws Exception {
        String admin = admin();
        mockMvc.perform(post("/api/users/me/become-seller").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"acceptedRules\":true}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ADMIN_ACCOUNT"))
                .andExpect(jsonPath("$.message", containsString("Admin accounts can't become sellers")));
        mockMvc.perform(post("/api/orders").header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"listingId\":\"00000000-0000-0000-0000-000000000001\",\"quantity\":1}],\"fulfilmentMethod\":\"PICKUP\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", containsString("Admin accounts can't place orders")));
    }

    @Test
    void editingSomeoneElsesListingIsA403ThatSaysWhy() throws Exception {
        String[] owner = seller();
        String listingId = product(owner[0], owner[1], "Rusks", 5);
        mockMvc.perform(patch("/api/listings/" + listingId).header("Authorization", seller()[0])
                        .contentType(MediaType.APPLICATION_JSON).content("{\"price\":1}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", containsString("Only the owner of this business")));
    }

    @Test
    void sellingRequiresAcceptingTheMarketplaceRulesFirst() throws Exception {
        String token = student();
        mockMvc.perform(post("/api/businesses").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"businessName\":\"Early Shop\",\"description\":\"Things\",\"category\":\"Other\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("accept the marketplace rules")));
    }

    @Test
    void suspendedStudentsAreLoggedOutStraightAway() throws Exception {
        String email = studentEmail("suspended");
        register(email);
        verify(email);
        String token = login(email, "Rules@1234");
        String userId = userRepository.findByEmail(email).orElseThrow().getId().toString();

        mockMvc.perform(post("/api/admin/users/" + userId + "/suspend").header("Authorization", admin())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"Selling alcohol\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/users/me").header("Authorization", token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", containsString("suspended")))
                .andExpect(jsonPath("$.message", containsString("Selling alcohol")));
    }

    @Test
    void sellerContactDetailsNeedALogin() throws Exception {
        String[] seller = seller();
        mockMvc.perform(get("/api/businesses/" + seller[1] + "/contact")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/businesses/" + seller[1] + "/contact").header("Authorization", student()))
                .andExpect(status().isOk());
    }

    // ---------------------------------------------------------------- restricted items

    @Test
    void restrictedItemsAreRefusedWithTheReason() throws Exception {
        String[] seller = seller();
        mockMvc.perform(post("/api/listings/products").header("Authorization", seller[0]).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"businessId\":\"%s\",\"name\":\"Vodka shots\",\"description\":\"Party pack\",\"category\":\"Food\",\"price\":50,\"stockQuantity\":3}"
                                .formatted(seller[1])))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("RESTRICTED_ITEM"))
                .andExpect(jsonPath("$.message", containsString("vodka")))
                .andExpect(jsonPath("$.message", containsString("suspended")));

        // Whole words only, plus everyday exceptions: these are fine.
        product(seller[0], seller[1], "Ginger beer and gingerbread", 4);
        product(seller[0], seller[1], "Hot glue gun for crafts", 2);
    }

    @Test
    void adminsCanTakeDownAListingAndItDisappears() throws Exception {
        String[] seller = seller();
        String listingId = product(seller[0], seller[1], "Mystery brownies", 6);
        String admin = admin();

        mockMvc.perform(post("/api/admin/listings/" + listingId + "/take-down").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"Edibles containing dagga\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/listings/" + listingId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("broke UniLinkHub's marketplace rules")));
        mockMvc.perform(get("/api/listings/" + listingId).header("Authorization", seller[0]))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.takedownReason").value("Edibles containing dagga"));
        mockMvc.perform(post("/api/listings/" + listingId + "/reactivate").header("Authorization", seller[0]))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", containsString("removed by an admin")));
        mockMvc.perform(get("/api/admin/listings").param("status", "REMOVED").header("Authorization", admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[?(@.listing.id=='" + listingId + "')]").exists());
    }

    // ---------------------------------------------------------------- honest trading

    @Test
    void sellersCantBuyFromOrReviewTheirOwnBusiness() throws Exception {
        String[] seller = seller();
        String listingId = product(seller[0], seller[1], "Muffins", 5);

        mockMvc.perform(post("/api/orders").header("Authorization", seller[0]).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"listingId\":\"%s\",\"quantity\":1}],\"fulfilmentMethod\":\"PICKUP\"}".formatted(listingId)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", containsString("your own business")));
        mockMvc.perform(post("/api/businesses/" + seller[1] + "/reviews").header("Authorization", seller[0])
                        .contentType(MediaType.APPLICATION_JSON).content("{\"rating\":5,\"comment\":\"Best shop ever\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", containsString("can't review your own business")));
        mockMvc.perform(post("/api/businesses/" + seller[1] + "/reviews").header("Authorization", student())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"rating\":1,\"comment\":\"Never bought here\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", containsString("after buying from it")));
    }

    @Test
    void cancellingAnOrderPutsTheStockBack() throws Exception {
        String[] seller = seller();
        String listingId = product(seller[0], seller[1], "Koeksisters", 5);
        String orderJson = mockMvc.perform(post("/api/orders").header("Authorization", student()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"listingId\":\"%s\",\"quantity\":5}],\"fulfilmentMethod\":\"PICKUP\"}".formatted(listingId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        mockMvc.perform(get("/api/listings/" + listingId)).andExpect(jsonPath("$.status").value("SOLD_OUT"));

        String orderId = objectMapper.readTree(orderJson).get(0).get("id").asText();
        mockMvc.perform(post("/api/orders/" + orderId + "/cancel").header("Authorization", seller[0]))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/listings/" + listingId))
                .andExpect(jsonPath("$.stockQuantity").value(5))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    // ---------------------------------------------------------------- images

    @Test
    void sellersCanUploadRealImagesOnly() throws Exception {
        String[] seller = seller();
        String json = mockMvc.perform(multipart("/api/images").file(new MockMultipartFile("file", "logo.png", "image/png", PNG))
                        .header("Authorization", seller[0]))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String url = objectMapper.readTree(json).get("url").asText();
        mockMvc.perform(get(url))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/png"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"));

        // A renamed text file is refused, whatever its name claims.
        mockMvc.perform(multipart("/api/images").file(new MockMultipartFile("file", "photo.jpg", "image/jpeg", "not an image".getBytes()))
                        .header("Authorization", seller[0]))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("isn't a supported image")));
        // Buyers who aren't sellers have nothing to upload images for.
        mockMvc.perform(multipart("/api/images").file(new MockMultipartFile("file", "logo.png", "image/png", PNG))
                        .header("Authorization", student()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", containsString("Only sellers can upload images")));
        // Listing photos must be uploaded, not links to other websites.
        mockMvc.perform(post("/api/listings/products").header("Authorization", seller[0]).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"businessId\":\"%s\",\"name\":\"Pens\",\"description\":\"Blue pens\",\"category\":\"Other\",\"price\":5,\"stockQuantity\":3,\"imageUrl\":\"https://example.com/x.png\"}"
                                .formatted(seller[1])))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("upload button")));
    }

    // ---------------------------------------------------------------- admin accounts

    @Test
    void adminsInviteNewAdminsWhoSetTheirOwnPassword() throws Exception {
        String inviteEmail = "new.admin." + unique() + "@cput.ac.za";
        mockMvc.perform(post("/api/admin/admins").header("Authorization", admin()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"New\",\"lastName\":\"Admin\",\"email\":\"%s\"}".formatted(inviteEmail)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.studentNumber").doesNotExist());

        String token = userRepository.findByEmail(inviteEmail).orElseThrow().getPasswordResetToken();
        mockMvc.perform(post("/api/auth/reset-password").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"%s\",\"newPassword\":\"Invited@1234\"}".formatted(token)))
                .andExpect(status().isNoContent());
        login(inviteEmail, "Invited@1234");
    }

    @Test
    void sellersCantFollowTheirOwnBusiness() throws Exception {
        String[] seller = seller();
        mockMvc.perform(post("/api/businesses/" + seller[1] + "/follow").header("Authorization", seller[0]))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", containsString("can't follow your own business")));

        mockMvc.perform(post("/api/businesses/" + seller[1] + "/follow").header("Authorization", student()))
                .andExpect(status().is2xxSuccessful());
    }

    @Test
    void brokenLinksGetAFriendlyMessageNotAServerError() throws Exception {
        mockMvc.perform(get("/api/listings/not-a-real-id"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("link doesn't look right")));
    }
}
