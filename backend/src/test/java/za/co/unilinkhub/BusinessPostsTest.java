package za.co.unilinkhub;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Business posts on provider pages: who can post, what followers see, and how posts are moderated. */
class BusinessPostsTest extends ApiTestSupport {

    private String publish(String token, String businessId, String body) throws Exception {
        String json = mockMvc.perform(post("/api/businesses/" + businessId + "/posts").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(java.util.Map.of("body", body))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.createdAt").isNotEmpty()) // must be set in the response itself, not only once saved
                .andReturn().getResponse().getContentAsString();
        return json(json).get("id").asText();
    }

    @Test
    void onlyTheOwnerOfAVerifiedBusinessCanPost() throws Exception {
        String[] pending = seller();
        mockMvc.perform(post("/api/businesses/" + pending[1] + "/posts").header("Authorization", pending[0])
                        .contentType(MediaType.APPLICATION_JSON).content("{\"body\":\"Hello res!\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", containsString("Only verified businesses can post")));

        String[] verified = verifiedSeller();
        mockMvc.perform(post("/api/businesses/" + verified[1] + "/posts").header("Authorization", student())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"body\":\"Not my shop\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", containsString("Only the owner of")));
        mockMvc.perform(post("/api/businesses/" + verified[1] + "/posts").header("Authorization", admin())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"body\":\"Admin post\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ADMIN_ACCOUNT"));

        publish(verified[0], verified[1], "Fresh muffins at 4pm today!");
        mockMvc.perform(get("/api/businesses/" + verified[1] + "/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].body").value("Fresh muffins at 4pm today!"))
                .andExpect(jsonPath("$[0].businessVerified").value(true));
    }

    @Test
    void postsFollowTheSameRestrictedItemsRules() throws Exception {
        String[] seller = verifiedSeller();
        mockMvc.perform(post("/api/businesses/" + seller[1] + "/posts").header("Authorization", seller[0])
                        .contentType(MediaType.APPLICATION_JSON).content("{\"body\":\"Cheap vapes, DM me\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("RESTRICTED_ITEM"));
        mockMvc.perform(post("/api/businesses/" + seller[1] + "/posts").header("Authorization", seller[0])
                        .contentType(MediaType.APPLICATION_JSON).content("{\"body\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Write something or add a photo")));
    }

    @Test
    void followersAreNotifiedAndSeePostsInTheirFeed() throws Exception {
        String[] seller = verifiedSeller();
        String follower = student();
        mockMvc.perform(post("/api/businesses/" + seller[1] + "/follow").header("Authorization", follower))
                .andExpect(status().is2xxSuccessful());

        publish(seller[0], seller[1], "New stock: A3 posters now in colour");

        mockMvc.perform(get("/api/notifications").header("Authorization", follower))
                .andExpect(jsonPath("$[?(@.category=='POST')].message", org.hamcrest.Matchers.hasItem(containsString("A3 posters"))));
        mockMvc.perform(get("/api/posts/feed").header("Authorization", follower))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].body").value("New stock: A3 posters now in colour"));
        mockMvc.perform(get("/api/posts/feed")).andExpect(status().isUnauthorized());
    }

    @Test
    void studentsCanLikeAndCommentAndOwnersModerateTheirPage() throws Exception {
        String[] seller = verifiedSeller();
        String postId = publish(seller[0], seller[1], "Which flavour should we bake next?");
        String fan = student();

        mockMvc.perform(post("/api/posts/" + postId + "/like").header("Authorization", fan))
                .andExpect(jsonPath("$.likeCount").value(1))
                .andExpect(jsonPath("$.likedByMe").value(true));
        mockMvc.perform(post("/api/posts/" + postId + "/like").header("Authorization", fan))
                .andExpect(jsonPath("$.likeCount").value(1)); // liking twice doesn't double-count
        mockMvc.perform(delete("/api/posts/" + postId + "/like").header("Authorization", fan))
                .andExpect(jsonPath("$.likeCount").value(0));

        String commentJson = mockMvc.perform(post("/api/posts/" + postId + "/comments").header("Authorization", fan)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"body\":\"Red velvet please!\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        String commentId = json(commentJson).get("id").asText();
        mockMvc.perform(get("/api/posts/" + postId + "/comments"))
                .andExpect(jsonPath("$[0].body").value("Red velvet please!"));

        // Another student can't delete it; the page owner can.
        mockMvc.perform(delete("/api/post-comments/" + commentId).header("Authorization", student()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", containsString("only delete your own comments")));
        mockMvc.perform(delete("/api/post-comments/" + commentId).header("Authorization", seller[0]))
                .andExpect(status().isNoContent());
    }

    @Test
    void pinnedPostComesFirst() throws Exception {
        String[] seller = verifiedSeller();
        String first = publish(seller[0], seller[1], "Opening hours: 2-6pm weekdays");
        publish(seller[0], seller[1], "Newer post");
        mockMvc.perform(post("/api/posts/" + first + "/pin").header("Authorization", seller[0])
                        .contentType(MediaType.APPLICATION_JSON).content("{\"pinned\":true}"))
                .andExpect(jsonPath("$.pinned").value(true));
        mockMvc.perform(get("/api/businesses/" + seller[1] + "/posts"))
                .andExpect(jsonPath("$[0].id").value(first));
    }

    @Test
    void reportedPostsCanBeRemovedByAdmins() throws Exception {
        String[] seller = verifiedSeller();
        String postId = publish(seller[0], seller[1], "Totally normal brownies");
        mockMvc.perform(post("/api/posts/" + postId + "/flag").header("Authorization", student()))
                .andExpect(status().isNoContent());

        String admin = admin();
        mockMvc.perform(get("/api/admin/posts").param("flaggedOnly", "true").header("Authorization", admin))
                .andExpect(jsonPath("$[?(@.id=='" + postId + "')].flagCount").value(org.hamcrest.Matchers.hasItem(1)));
        mockMvc.perform(post("/api/admin/posts/" + postId + "/remove").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"Advertising dagga edibles\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/businesses/" + seller[1] + "/posts"))
                .andExpect(jsonPath("$[?(@.id=='" + postId + "')]").doesNotExist());
        mockMvc.perform(get("/api/businesses/" + seller[1] + "/posts").header("Authorization", seller[0]))
                .andExpect(jsonPath("$[0].removedReason").value("Advertising dagga edibles"));
        mockMvc.perform(post("/api/posts/" + postId + "/like").header("Authorization", student()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("removed")));
    }

    @Test
    void businessesHaveADailyPostLimit() throws Exception {
        String[] seller = verifiedSeller();
        for (int i = 0; i < 10; i++) {
            publish(seller[0], seller[1], "Update number " + i);
        }
        mockMvc.perform(post("/api/businesses/" + seller[1] + "/posts").header("Authorization", seller[0])
                        .contentType(MediaType.APPLICATION_JSON).content("{\"body\":\"One too many\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.message", containsString("daily limit")));
    }
}
