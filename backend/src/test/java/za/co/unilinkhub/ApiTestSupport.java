package za.co.unilinkhub;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import za.co.unilinkhub.user.domain.User;
import za.co.unilinkhub.user.repository.UserRepository;

import java.util.concurrent.atomic.AtomicInteger;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Shared set-up for HTTP-level tests: creates verified students, sellers and admin accounts the same
 * way real people would (register, follow the emailed link, log in) and returns "Bearer ..." tokens.
 */
@SpringBootTest
@AutoConfigureMockMvc
abstract class ApiTestSupport {

    private static final AtomicInteger SEQ = new AtomicInteger(500);

    @Autowired protected MockMvc mockMvc;
    @Autowired protected UserRepository userRepository;
    @Autowired protected PasswordEncoder passwordEncoder;
    @Autowired protected ObjectMapper objectMapper;

    protected String unique() {
        return String.valueOf(SEQ.incrementAndGet());
    }

    protected String studentEmail(String name) {
        return name + "." + unique() + "@mycput.ac.za";
    }

    protected JsonNode json(String body) throws Exception {
        return objectMapper.readTree(body);
    }

    protected void register(String email) throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                        {"studentNumber":"77%s","firstName":"Test","lastName":"Student","email":"%s","password":"Rules@1234"}
                        """.formatted(unique(), email)))
                .andExpect(status().isCreated());
    }

    /** Follows the link the verification email would have carried. */
    protected void verify(String email) throws Exception {
        User user = userRepository.findByEmail(email).orElseThrow();
        mockMvc.perform(get("/api/auth/verify").param("token", user.getVerificationToken())).andExpect(status().isOk());
    }

    protected String login(String email, String password) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + json(body).get("token").asText();
    }

    protected String student() throws Exception {
        String email = studentEmail("student");
        register(email);
        verify(email);
        return login(email, "Rules@1234");
    }

    protected String admin() throws Exception {
        String email = "admin." + unique() + "@cput.ac.za";
        userRepository.save(User.createAdmin("Test", "Admin", email, passwordEncoder.encode("Admin@1234")));
        return login(email, "Admin@1234");
    }

    /** A verified student who has accepted the rules and owns one (pending) business. Returns [token, businessId]. */
    protected String[] seller() throws Exception {
        return seller("BELLVILLE");
    }

    /** A seller whose business is on the given campus. */
    protected String[] seller(String campus) throws Exception {
        String token = student();
        mockMvc.perform(post("/api/users/me/become-seller").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"acceptedRules\":true}"))
                .andExpect(status().isOk());
        String body = mockMvc.perform(post("/api/businesses").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"businessName\":\"Rules Shop %s\",\"description\":\"Snacks and stationery\",\"category\":\"Food\",\"campus\":\"%s\",\"pickupLocation\":\"Res block C\"}"
                                .formatted(unique(), campus)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return new String[]{token, json(body).get("id").asText()};
    }

    /** Like {@link #seller()}, but an admin has already verified the business. */
    protected String[] verifiedSeller() throws Exception {
        String[] seller = seller();
        mockMvc.perform(post("/api/admin/businesses/" + seller[1] + "/verify").header("Authorization", admin()))
                .andExpect(status().isOk());
        return seller;
    }

    protected String product(String sellerToken, String businessId, String name, int stock) throws Exception {
        String body = mockMvc.perform(post("/api/listings/products").header("Authorization", sellerToken)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"businessId":"%s","name":"%s","description":"Fresh from res","category":"Food","price":20,"stockQuantity":%d}
                                """.formatted(businessId, name, stock)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return json(body).get("id").asText();
    }
}
