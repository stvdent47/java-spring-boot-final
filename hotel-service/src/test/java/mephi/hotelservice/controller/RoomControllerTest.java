package mephi.hotelservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import mephi.hotelservice.dto.AvailabilityRequest;
import mephi.hotelservice.dto.AvailabilityResponse;
import mephi.hotelservice.dto.RoomRequest;
import mephi.hotelservice.dto.RoomResponse;
import mephi.hotelservice.entity.RoomType;
import mephi.hotelservice.exception.DuplicateResourceException;
import mephi.hotelservice.exception.ResourceNotFoundException;
import mephi.hotelservice.service.RoomService;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

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

@WebMvcTest(RoomController.class)
@Import(RoomControllerTest.TestSecurityConfig.class)
@DisplayName("RoomController MockMvc Tests")
class RoomControllerTest {
    @org.springframework.boot.test.context.TestConfiguration
    @org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
    static class TestSecurityConfig {
        @Bean
        public SecurityFilterChain roomTestSecurityFilterChain(HttpSecurity http) throws Exception {
            http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                    .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                    .requestMatchers(HttpMethod.GET, "/rooms/**").permitAll()
                    .requestMatchers("/rooms/*/confirm-availability").authenticated()
                    .requestMatchers("/rooms/*/release").authenticated()
                    .requestMatchers(HttpMethod.POST, "/rooms/**").hasRole("ADMIN")
                    .requestMatchers(HttpMethod.PUT, "/rooms/**").hasRole("ADMIN")
                    .requestMatchers(HttpMethod.DELETE, "/rooms/**").hasRole("ADMIN")
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
    private RoomService roomService;

    private RoomResponse testResponse;
    private RoomRequest testRequest;

    @BeforeEach
    void setUp() {
        testResponse = RoomResponse.builder()
            .id(1L)
            .roomNumber("101")
            .roomType(RoomType.STANDARD)
            .pricePerNight(BigDecimal.valueOf(100))
            .maxOccupancy(2)
            .available(true)
            .hotelId(1L)
            .hotelName("Grand Hotel")
            .build();

        testRequest = new RoomRequest();
        testRequest.setHotelId(1L);
        testRequest.setRoomNumber("101");
        testRequest.setRoomType(RoomType.STANDARD);
        testRequest.setPricePerNight(BigDecimal.valueOf(100));
        testRequest.setMaxOccupancy(2);
    }

    @Nested
    @DisplayName("GET /rooms")
    class GetAllRooms {
        @Test
        @DisplayName("should return all rooms (public endpoint)")
        void should_ReturnAllRooms() throws Exception {
            given(roomService.getAllRooms()).willReturn(List.of(testResponse));

            mockMvc.perform(get("/rooms"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].roomNumber", is("101")))
                .andExpect(jsonPath("$[0].roomType", is("STANDARD")));
        }
    }

    @Nested
    @DisplayName("GET /rooms/{id}")
    class GetRoomById {
        @Test
        @DisplayName("should return room when found (public endpoint)")
        void should_ReturnRoom_When_Found() throws Exception {
            given(roomService.getRoomById(1L)).willReturn(testResponse);

            mockMvc.perform(get("/rooms/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.roomNumber", is("101")));
        }

        @Test
        @DisplayName("should return 404 when room not found")
        void should_Return404_When_RoomNotFound() throws Exception {
            given(roomService.getRoomById(999L))
                .willThrow(new ResourceNotFoundException("Room", 999L));

            mockMvc.perform(get("/rooms/999"))
                .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /rooms/hotel/{hotelId}")
    class GetRoomsByHotelId {
        @Test
        @DisplayName("should return rooms for hotel (public endpoint)")
        void should_ReturnRoomsForHotel() throws Exception {
            given(roomService.getRoomsByHotelId(1L)).willReturn(List.of(testResponse));

            mockMvc.perform(get("/rooms/hotel/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].hotelId", is(1)));
        }

        @Test
        @DisplayName("should return 404 when hotel not found")
        void should_Return404_When_HotelNotFound() throws Exception {
            given(roomService.getRoomsByHotelId(999L))
                .willThrow(new ResourceNotFoundException("Hotel", 999L));

            mockMvc.perform(get("/rooms/hotel/999"))
                .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /rooms/available")
    class GetAvailableRooms {
        @Test
        @DisplayName("should return available rooms without filters (public endpoint)")
        void should_ReturnAvailableRooms_WithoutFilters() throws Exception {
            given(roomService.getAvailableRooms()).willReturn(List.of(testResponse));

            mockMvc.perform(get("/rooms/available"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].available", is(true)));
        }

        @Test
        @DisplayName("should filter by hotel ID")
        void should_FilterByHotelId() throws Exception {
            given(roomService.getAvailableRoomsByHotelId(1L)).willReturn(List.of(testResponse));

            mockMvc.perform(get("/rooms/available").param("hotelId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].hotelId", is(1)));
        }

        @Test
        @DisplayName("should filter by room type")
        void should_FilterByRoomType() throws Exception {
            given(roomService.getAvailableRoomsByType(RoomType.STANDARD)).willReturn(List.of(testResponse));

            mockMvc.perform(get("/rooms/available").param("roomType", "STANDARD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].roomType", is("STANDARD")));
        }

        @Test
        @DisplayName("should filter by guest count")
        void should_FilterByGuestCount() throws Exception {
            given(roomService.getAvailableRoomsByCapacity(2)).willReturn(List.of(testResponse));

            mockMvc.perform(get("/rooms/available").param("guestCount", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].maxOccupancy", greaterThanOrEqualTo(2)));
        }

        @Test
        @DisplayName("should combine filters")
        void should_CombineFilters() throws Exception {
            given(roomService.getAvailableRoomsByHotelIdAndType(1L, RoomType.STANDARD))
                .willReturn(List.of(testResponse));

            mockMvc.perform(
                get("/rooms/available")
                    .param("hotelId", "1")
                    .param("roomType", "STANDARD")
            )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].hotelId", is(1)))
                .andExpect(jsonPath("$[0].roomType", is("STANDARD")));
        }
    }

    @Nested
    @DisplayName("POST /rooms")
    class CreateRoom {
        @Test
        @DisplayName("should create room when admin and valid request")
        void should_CreateRoom_When_AdminAndValidRequest() throws Exception {
            given(roomService.createRoom(any(RoomRequest.class))).willReturn(testResponse);

            UserDetails adminUser = User.builder()
                .username("admin")
                .password("password")
                .roles("ADMIN")
                .build();

            mockMvc.perform(
                post("/rooms")
                    .with(csrf())
                    .with(user(adminUser))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(testRequest))
            )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.roomNumber", is("101")));
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
                post("/rooms")
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
            mockMvc.perform(
                post("/rooms")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(testRequest))
            )
                .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("should return 409 when room number already exists")
        void should_Return409_When_RoomNumberExists() throws Exception {
            given(roomService.createRoom(any(RoomRequest.class)))
                .willThrow(new DuplicateResourceException("Room", "room number in hotel", "101"));

            UserDetails adminUser = User.builder()
                .username("admin")
                .password("password")
                .roles("ADMIN")
                .build();

            mockMvc.perform(
                post("/rooms")
                    .with(csrf())
                    .with(user(adminUser))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(testRequest))
            )
                .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("should return 404 when hotel not found")
        void should_Return404_When_HotelNotFound() throws Exception {
            given(roomService.createRoom(any(RoomRequest.class)))
                .willThrow(new ResourceNotFoundException("Hotel", 999L));

            UserDetails adminUser = User.builder()
                .username("admin")
                .password("password")
                .roles("ADMIN")
                .build();

            mockMvc.perform(
                post("/rooms")
                    .with(csrf())
                    .with(user(adminUser))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(testRequest))
            )
                .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PUT /rooms/{id}")
    class UpdateRoom {
        @Test
        @DisplayName("should update room when admin and valid request")
        void should_UpdateRoom_When_AdminAndValidRequest() throws Exception {
            given(roomService.updateRoom(eq(1L), any(RoomRequest.class))).willReturn(testResponse);

            UserDetails adminUser = User.builder()
                .username("admin")
                .password("password")
                .roles("ADMIN")
                .build();

            mockMvc.perform(
                put("/rooms/1")
                    .with(csrf())
                    .with(user(adminUser))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(testRequest))
            )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roomNumber", is("101")));
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
                put("/rooms/1")
                    .with(csrf())
                    .with(user(regularUser))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(testRequest))
            )
                .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("DELETE /rooms/{id}")
    class DeleteRoom {
        @Test
        @DisplayName("should delete room when admin")
        void should_DeleteRoom_When_Admin() throws Exception {
            willDoNothing().given(roomService).deleteRoom(1L);

            UserDetails adminUser = User.builder()
                .username("admin")
                .password("password")
                .roles("ADMIN")
                .build();

            mockMvc.perform(
                delete("/rooms/1")
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
                delete("/rooms/1")
                    .with(csrf())
                    .with(user(regularUser))
            )
                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 404 when room not found")
        void should_Return404_When_RoomNotFound() throws Exception {
            willThrow(new ResourceNotFoundException("Room", 999L)).given(roomService).deleteRoom(999L);

            UserDetails adminUser = User.builder()
                .username("admin")
                .password("password")
                .roles("ADMIN")
                .build();

            mockMvc.perform(
                delete("/rooms/999")
                    .with(csrf())
                    .with(user(adminUser))
            )
                .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /rooms/{id}/confirm-availability (Internal API)")
    class ConfirmAvailability {
        @Test
        @DisplayName("should confirm availability when room is available")
        void should_ConfirmAvailability_When_RoomAvailable() throws Exception {
            String requestId = UUID.randomUUID().toString();
            AvailabilityRequest availRequest = new AvailabilityRequest();
            availRequest.setRequestId(requestId);
            availRequest.setStartDate(LocalDate.now().plusDays(1));
            availRequest.setEndDate(LocalDate.now().plusDays(3));
            availRequest.setGuestCount(2);

            AvailabilityResponse availResponse = AvailabilityResponse.builder()
                .roomId(1L)
                .hotelId(1L)
                .requestId(requestId)
                .confirmed(true)
                .message("Room availability confirmed")
                .totalPrice(BigDecimal.valueOf(200))
                .nights(2)
                .build();

            given(roomService.confirmAvailability(eq(1L), any(AvailabilityRequest.class)))
                .willReturn(availResponse);

            UserDetails authUser = User.builder()
                .username("service")
                .password("password")
                .roles("USER")
                .build();

            mockMvc.perform(
                post("/rooms/1/confirm-availability")
                    .with(csrf())
                    .with(user(authUser))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(availRequest))
            )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.confirmed", is(true)))
                .andExpect(jsonPath("$.totalPrice", is(200)))
                .andExpect(jsonPath("$.nights", is(2)));
        }

        @Test
        @DisplayName("should return not confirmed when room is not available")
        void should_ReturnNotConfirmed_When_RoomNotAvailable() throws Exception {
            String requestId = UUID.randomUUID().toString();
            AvailabilityRequest availRequest = new AvailabilityRequest();
            availRequest.setRequestId(requestId);
            availRequest.setStartDate(LocalDate.now().plusDays(1));
            availRequest.setEndDate(LocalDate.now().plusDays(3));

            AvailabilityResponse availResponse = AvailabilityResponse.builder()
                .roomId(1L)
                .requestId(requestId)
                .confirmed(false)
                .message("Room is not available")
                .build();

            given(roomService.confirmAvailability(eq(1L), any(AvailabilityRequest.class)))
                .willReturn(availResponse);

            UserDetails authUser = User.builder()
                .username("service")
                .password("password")
                .roles("USER")
                .build();

            mockMvc.perform(
                post("/rooms/1/confirm-availability")
                    .with(csrf())
                    .with(user(authUser))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(availRequest))
            )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.confirmed", is(false)));
        }
    }

    @Nested
    @DisplayName("POST /rooms/{id}/release (Internal API)")
    class ReleaseRoom {
        @Test
        @DisplayName("should release room successfully")
        void should_ReleaseRoom_Successfully() throws Exception {
            String requestId = UUID.randomUUID().toString();
            AvailabilityResponse releaseResponse = AvailabilityResponse.builder()
                .roomId(1L)
                .requestId(requestId)
                .confirmed(false)
                .message("Room released successfully")
                .build();

            given(roomService.releaseRoom(1L, requestId)).willReturn(releaseResponse);

            UserDetails authUser = User.builder()
                .username("service")
                .password("password")
                .roles("USER")
                .build();

            mockMvc.perform(
                post("/rooms/1/release")
                    .with(csrf())
                    .with(user(authUser))
                    .param("requestId", requestId)
            )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", containsString("released")));
        }
    }
}
