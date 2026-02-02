package com.jiralite.backend.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;

import com.jiralite.backend.entity.NotificationEntity;
import com.jiralite.backend.service.NotificationService;

@ExtendWith(SpringExtension.class)
@WebMvcTest(NotificationController.class)
@AutoConfigureMockMvc(addFilters = false)
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NotificationService notificationService;

    @Test
    void list_returnsOk() throws Exception {
        NotificationEntity n = new NotificationEntity();
        n.setId(UUID.randomUUID());
        n.setContent("hello");
        when(notificationService.listForCurrentUser()).thenReturn(List.of(n));

        mockMvc.perform(get("/notifications").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void markRead_noContent() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(patch("/notifications/{id}/read", id))
                .andExpect(status().isNoContent());

        verify(notificationService).markRead(id);
    }
}
