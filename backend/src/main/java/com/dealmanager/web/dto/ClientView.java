package com.dealmanager.web.dto;

import com.dealmanager.domain.Client;

public record ClientView(Long id, String name, String segment) {

    public static ClientView from(Client client) {
        return new ClientView(client.getId(), client.getName(), client.getSegment());
    }
}
