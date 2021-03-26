package com.vi.realabs.websocket;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Room {
    private Map<String, Member> members;
    private List<Chat> chats;
}
