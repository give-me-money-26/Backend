package kr.ac.catholic.aeroharvest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "aeroharvest.cors.allowed-origins=https://aero-harvest.vercel.app")
@AutoConfigureMockMvc
class SwaggerCorsTests {

    private static final String BODY = """
            {"buildingId":"NICHOLLS","sourceMode":"TIME_SLOT","hour":15,
             "collectionAreaM2":0.8,"efficiencyPercent":25,"distanceM":2,"angleDegrees":0,
             "operatingHoursPerDay":8,"operatingDaysPerYear":365,
             "tariffKrwPerKwh":150,"carbonFactorKgPerKwh":0.4}
            """;

    @Autowired
    MockMvc mockMvc;

    @Test
    void swaggerOriginCanPostEvenWhenFrontendOriginsAreOverridden() throws Exception {
        mockMvc.perform(post("/api/v1/simulations")
                        .header("Origin", "https://aeromap.khoon.net")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "https://aeromap.khoon.net"));
    }

    @Test
    void frontendOriginStillAllowed() throws Exception {
        mockMvc.perform(post("/api/v1/simulations")
                        .header("Origin", "https://aero-harvest.vercel.app")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isOk());
    }

    @Test
    void unknownOriginIsRejected() throws Exception {
        mockMvc.perform(post("/api/v1/simulations")
                        .header("Origin", "https://evil.example.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isForbidden());
    }
}
