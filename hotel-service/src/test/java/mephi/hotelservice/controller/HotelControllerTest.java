package mephi.hotelservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import mephi.hotelservice.dto.HotelRequest;
import mephi.hotelservice.dto.HotelResponse;
import mephi.hotelservice.exception.DuplicateResourceException;
import mephi.hotelservice.exception.ResourceNotFoundException;
import mephi.hotelservice.service.HotelService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(HotelController.class)
@Import(HotelControllerTest.TestSecurityConfig.class)
@DisplayName("HotelController MockMvc Tests")
class HotelControllerTest {
    @org.springframework.boot.test.context.TestConfiguration
    @org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
    static class TestSecurityConfig {
        @Bean
        public SecurityFilterChain hotelTestSecurityFilterChain(HttpSecurity http) throws Exception {
            http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                    .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                    .requestMatchers(HttpMethod.GET, "/hotels/**").permitAll()
                    .requestMatchers(HttpMethod.POST, "/hotels/**").hasRole("ADMIN")
                    .requestMatchers(HttpMethod.PUT, "/hotels/**").hasRole("ADMIN")
                    .requestMatchers(HttpMethod.DELETE, "/hotels/**").hasRole("ADMIN")
                    .anyRequest().authenticated()
                )
                .httpBasic(basic -> {});

            return http.build();
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private HotelService hotelService;

    private HotelResponse testResponse;
    private HotelRequest testRequest;

    @BeforeEach
    void setUp() {
        testResponse = HotelResponse.builder()
            .id(1L)
            .name("Grand Hotel")
            .city("Moscow")
            .country("Russia")
            .address("Red Square 1")
            .starRating(5)
            .build();

        testRequest = new HotelRequest(
            "Grand Hotel",
            "Red Square 1",
            "Moscow",
            "Russia",
            5
        );
    }

    @Nested
    @DisplayName("GET /hotels")
    class GetAllHotels {
        @Test
        @DisplayName("should return all hotels (public endpoint)")
        void should_ReturnAllHotels_When_HotelsExist() throws Exception {
            given(hotelService.getAllHotels()).willReturn(List.of(testResponse));

            mockMvc.perform(get("/hotels"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", is("Grand Hotel")))
                .andExpect(jsonPath("$[0].city", is("Moscow")));
        }

        @Test
        @DisplayName("should return empty list when no hotels")
        void should_ReturnEmptyList_When_NoHotels() throws Exception {
            given(hotelService.getAllHotels()).willReturn(List.of());

            mockMvc.perform(get("/hotels"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    @Nested
    @DisplayName("GET /hotels/{id}")
    class GetHotelById {
        @Test
        @DisplayName("should return hotel when found (public endpoint)")
        void should_ReturnHotel_When_Found() throws Exception {
            given(hotelService.getHotelById(1L)).willReturn(testResponse);

            mockMvc.perform(get("/hotels/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.name", is("Grand Hotel")));
        }

        @Test
        @DisplayName("should return 404 when hotel not found")
        void should_Return404_When_HotelNotFound() throws Exception {
            given(hotelService.getHotelById(999L))
                .willThrow(new ResourceNotFoundException("Hotel", 999L));

            mockMvc.perform(get("/hotels/999"))
                .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /hotels/city/{city}")
    class GetHotelsByCity {
        @Test
        @DisplayName("should return hotels in city (public endpoint)")
        void should_ReturnHotelsInCity() throws Exception {
            given(hotelService.getHotelsByCity("Moscow")).willReturn(List.of(testResponse));

            mockMvc.perform(get("/hotels/city/Moscow"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].city", is("Moscow")));
        }
    }

    @Nested
    @DisplayName("GET /hotels/search")
    class SearchHotels {
        @Test
        @DisplayName("should return matching hotels (public endpoint)")
        void should_ReturnMatchingHotels() throws Exception {
            given(hotelService.searchHotelsByName("Grand")).willReturn(List.of(testResponse));

            mockMvc.perform(get("/hotels/search").param("name", "Grand"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", containsString("Grand")));
        }
    }

    @Nested
    @DisplayName("POST /hotels")
    class CreateHotel {
        @Test
        @DisplayName("should create hotel when admin and valid request")
        void should_CreateHotel_When_AdminAndValidRequest() throws Exception {
            given(hotelService.createHotel(any(HotelRequest.class))).willReturn(testResponse);

            UserDetails adminUser = User.builder()
                .username("admin")
                .password("password")
                .roles("ADMIN")
                .build();

            // Act & Assert
            mockMvc.perform(
                post("/hotels")
                    .with(csrf())
                    .with(user(adminUser))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(testRequest))
            )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("Grand Hotel")));
        }

        @Test
        @DisplayName("should return 403 when user is not admin")
        void should_Return403_When_NotAdmin() throws Exception {
            UserDetails regularUser = User.builder()
                .username("user")
                .password("password")
                .roles("USER")
                .build();

            mockMvc.perform(
                post("/hotels")
                    .with(csrf())
                    .with(user(regularUser))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(testRequest))
            )
                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 401 when not authenticated")
        void should_Return401_When_NotAuthenticated() throws Exception {
            mockMvc.perform(post("/hotels")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testRequest))
            )
                .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("should return 409 when hotel already exists")
        void should_Return409_When_HotelExists() throws Exception {
            given(hotelService.createHotel(any(HotelRequest.class)))
                .willThrow(new DuplicateResourceException("Hotel", "name and address", "Grand Hotel at Red Square 1"));

            UserDetails adminUser = User.builder()
                .username("admin")
                .password("password")
                .roles("ADMIN")
                .build();

            mockMvc.perform(
                post("/hotels")
                    .with(csrf())
                    .with(user(adminUser))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(testRequest))
            )
                .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("should return 400 when request is invalid")
        void should_Return400_When_InvalidRequest() throws Exception {
            HotelRequest invalidRequest = new HotelRequest(
                "", // Empty name - should fail validation
                "Red Square 1",
                "Moscow",
                "Russia",
                5
            );

            UserDetails adminUser = User.builder()
                .username("admin")
                .password("password")
                .roles("ADMIN")
                .build();

            mockMvc.perform(
                post("/hotels")
                    .with(csrf())
                    .with(user(adminUser))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidRequest))
            )
                .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("PUT /hotels/{id}")
    class UpdateHotel {
        @Test
        @DisplayName("should update hotel when admin and valid request")
        void should_UpdateHotel_When_AdminAndValidRequest() throws Exception {
            given(hotelService.updateHotel(eq(1L), any(HotelRequest.class))).willReturn(testResponse);

            UserDetails adminUser = User.builder()
                .username("admin")
                .password("password")
                .roles("ADMIN")
                .build();

            mockMvc.perform(
                put("/hotels/1")
                    .with(csrf())
                    .with(user(adminUser))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(testRequest))
            )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Grand Hotel")));
        }

        @Test
        @DisplayName("should return 403 when user is not admin")
        void should_Return403_When_NotAdmin() throws Exception {
            UserDetails regularUser = User.builder()
                .username("user")
                .password("password")
                .roles("USER")
                .build();

            mockMvc.perform(
                put("/hotels/1")
                    .with(csrf())
                    .with(user(regularUser))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(testRequest))
            )
                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 404 when hotel not found")
        void should_Return404_When_HotelNotFound() throws Exception {
            given(hotelService.updateHotel(eq(999L), any(HotelRequest.class)))
                .willThrow(new ResourceNotFoundException("Hotel", 999L));

            UserDetails adminUser = User.builder()
                .username("admin")
                .password("password")
                .roles("ADMIN")
                .build();

            mockMvc.perform(
                put("/hotels/999")
                    .with(csrf())
                    .with(user(adminUser))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(testRequest))
            )
                .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /hotels/{id}")
    class DeleteHotel {
        @Test
        @DisplayName("should delete hotel when admin")
        void should_DeleteHotel_When_Admin() throws Exception {
            willDoNothing().given(hotelService).deleteHotel(1L);

            UserDetails adminUser = User.builder()
                .username("admin")
                .password("password")
                .roles("ADMIN")
                .build();

            mockMvc.perform(
                delete("/hotels/1")
                    .with(csrf())
                    .with(user(adminUser))
            )
                .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("should return 403 when user is not admin")
        void should_Return403_When_NotAdmin() throws Exception {
            UserDetails regularUser = User.builder()
                .username("user")
                .password("password")
                .roles("USER")
                .build();

            mockMvc.perform(
                delete("/hotels/1")
                    .with(csrf())
                    .with(user(regularUser))
            )
                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 404 when hotel not found")
        void should_Return404_When_HotelNotFound() throws Exception {
            willThrow(new ResourceNotFoundException("Hotel", 999L)).given(hotelService).deleteHotel(999L);

            UserDetails adminUser = User.builder()
                .username("admin")
                .password("password")
                .roles("ADMIN")
                .build();

            mockMvc.perform(
                delete("/hotels/999")
                    .with(csrf())
                    .with(user(adminUser))
            )
                .andExpect(status().isNotFound());
        }
    }
}
