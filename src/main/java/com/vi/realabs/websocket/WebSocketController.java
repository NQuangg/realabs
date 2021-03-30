package com.vi.realabs.websocket;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.FieldValue;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

import java.util.*;
import java.util.concurrent.ExecutionException;

@Controller
public class WebSocketController {
    Firestore db = FirestoreClient.getFirestore();

    @MessageMapping("/room/init/{roomId}")
    @SendTo("/room/{roomId}")
    public LabInfo send(Member message, @DestinationVariable String roomId) throws ExecutionException, InterruptedException {
        DocumentReference docRef = db.collection("rooms").document(roomId);
        ApiFuture<DocumentSnapshot> documentSnapshot = docRef.get();
        DocumentSnapshot document = documentSnapshot.get();
        if (!document.exists()) {
            Member member = new Member(message.getUserId(), message.getUserName(), 0, 1, true);
            Map<String, Member> map = new HashMap<>();
            map.put(message.getUserId(), member);
            docRef.set(new Room(map, new ArrayList<>()));
        } else {
            Map<String, Member> map = document.toObject(Room.class).getMembers();
            Member member;
            if (map.get(message.getUserId()) == null) {
                member = new Member(message.getUserId(), message.getUserName(), 0, 1, true);
            } else {
                member = map.get(message.getUserId());
                member.increaseTabs();
            }
            docRef.update("members."+message.getUserId(), member);
        }

        return new LabInfo(getMembersMap(roomId), getChats(roomId));
    }

    @MessageMapping("/room/close/{roomId}")
    @SendTo("/room/{roomId}")
    public String close(Member message, @DestinationVariable String roomId) throws ExecutionException, InterruptedException {
        DocumentReference docRef = db.collection("rooms").document(roomId);
        ApiFuture<DocumentSnapshot> documentSnapshot = docRef.get();
        DocumentSnapshot document = documentSnapshot.get();
        Map<String, Member> map = document.toObject(Room.class).getMembers();
        Member member = map.get(message.getUserId());
        member.decreaseTabs();
        docRef.update("members."+message.getUserId(), member);

        return null;
    }

    @MessageMapping("/member/changeStep/{roomId}")
    @SendTo("/room/member/{roomId}")
    public Map<Integer, List<String>> changeStep(Member message, @DestinationVariable String roomId) throws ExecutionException, InterruptedException {
        DocumentReference docRef = db.collection("rooms").document(roomId);
        ApiFuture<DocumentSnapshot> documentSnapshot = docRef.get();
        DocumentSnapshot document = documentSnapshot.get();
        Map<String, Member> map = document.toObject(Room.class).getMembers();
        Member member = map.get(message.getUserId());
        member.setStep(message.getStep());
        docRef.update("members."+message.getUserId(), member);

        return getMembersMap(roomId);
    }

    @MessageMapping("/chat/addMessage/{roomId}")
    @SendTo("/room/chat/{roomId}")
    public Chat addChat(Chat message, @DestinationVariable String roomId) throws ExecutionException, InterruptedException {
        DocumentReference docRef = db.collection("rooms").document(roomId);
        docRef.update("chats", FieldValue.arrayUnion(message));

        return message;
    }

    private Map<Integer, List<String>> getMembersMap(String roomId) throws ExecutionException, InterruptedException {
        Thread.sleep(50);
        DocumentReference docRef = db.collection("rooms").document(roomId);
        ApiFuture<DocumentSnapshot> documentSnapshot = docRef.get();
        DocumentSnapshot document = documentSnapshot.get();
        Map<String, Member> map = document.toObject(Room.class).getMembers();

        Map<Integer, List<String>> membersMap = new HashMap<>();
        map.forEach((key, value) -> {
            int step = value.getStep();
            if (membersMap.get(step) == null) {
                membersMap.put(step, new ArrayList<>());
            }

            if (value.isOnline() == true) {
                membersMap.get(step).add(value.getUserName());
            }
        });

        return membersMap;
    }

    private List<Chat> getChats(String roomId) throws ExecutionException, InterruptedException {
        DocumentReference docRef = db.collection("rooms").document(roomId);
        ApiFuture<DocumentSnapshot> documentSnapshot = docRef.get();
        DocumentSnapshot document = documentSnapshot.get();
        List<Chat> chats = document.toObject(Room.class).getChats();

        return chats;
    }
}
