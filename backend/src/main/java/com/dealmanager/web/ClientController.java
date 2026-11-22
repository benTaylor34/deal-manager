package com.dealmanager.web;

import com.dealmanager.repository.ClientRepository;
import com.dealmanager.web.dto.ClientView;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ClientController {

    private final ClientRepository clients;

    public ClientController(ClientRepository clients) {
        this.clients = clients;
    }

    @GetMapping("/clients")
    public List<ClientView> clients() {
        return clients.findAll(Sort.by("name")).stream().map(ClientView::from).toList();
    }

    /** Lets the UI show who is signed in and which buttons to offer. */
    @GetMapping("/me")
    public Map<String, Object> me(Authentication auth) {
        List<String> roles = auth.getAuthorities().stream()
                .map(a -> a.getAuthority().replaceFirst("^ROLE_", ""))
                .toList();
        return Map.of("username", auth.getName(), "roles", roles);
    }
}
