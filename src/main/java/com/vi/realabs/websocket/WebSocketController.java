package com.vi.realabs.websocket;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.WriteResult;
import com.google.firebase.cloud.FirestoreClient;
import com.google.firebase.database.*;
import com.google.gson.Gson;
import com.vi.realabs.model.firestore.User;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import org.springframework.web.util.HtmlUtils;

import java.lang.reflect.Array;
import java.util.*;
import java.util.concurrent.ExecutionException;

@Controller
public class WebSocketController {
    Firestore db = FirestoreClient.getFirestore();

    @MessageMapping("/init/{roomId}")
    @SendTo("/room/{roomId}")
    public Map<Integer, List<String>> send(Member message, @DestinationVariable String roomId) throws ExecutionException, InterruptedException {
        DocumentReference docRef = db.collection("rooms").document(roomId);
        ApiFuture<DocumentSnapshot> documentSnapshot = docRef.get();
        DocumentSnapshot document = documentSnapshot.get();
        if (!document.exists()) {
            Member member = new Member(message.getUserId(), message.getUserName(), 0, 1, true);
            Map<String, Member> map = new HashMap<>();
            map.put(message.getUserId(), member);
            docRef.set(new MemberWrapper(map));
        } else {
            Map<String, Member> map = document.toObject(MemberWrapper.class).getMembers();
            Member member;
            if (map.get(message.getUserId()) == null) {
                member = new Member(message.getUserId(), message.getUserName(), 0, 1, true);
            } else {
                member = map.get(message.getUserId());
                member.increaseTabs();
            }
            docRef.update("members."+message.getUserId(), member);
        }

        Thread.sleep(50);
        return getLabInfo(roomId);
    }

    @MessageMapping("/changeStep/{roomId}")
    @SendTo("/room/{roomId}")
    public Map<Integer, List<String>> changeStep(Member message, @DestinationVariable String roomId) throws ExecutionException, InterruptedException {
        DocumentReference docRef = db.collection("rooms").document(roomId);
        ApiFuture<DocumentSnapshot> documentSnapshot = docRef.get();
        DocumentSnapshot document = documentSnapshot.get();
        Map<String, Member> map = document.toObject(MemberWrapper.class).getMembers();
        Member member = map.get(message.getUserId());
        member.setStep(message.getStep());
        docRef.update("members."+message.getUserId(), member);

        Thread.sleep(50);
        return getLabInfo(roomId);
    }

    @MessageMapping("/close/{roomId}")
    @SendTo("/room/{roomId}")
    public String close(Member message, @DestinationVariable String roomId) throws ExecutionException, InterruptedException {
        DocumentReference docRef = db.collection("rooms").document(roomId);
        ApiFuture<DocumentSnapshot> documentSnapshot = docRef.get();
        DocumentSnapshot document = documentSnapshot.get();
        Map<String, Member> map = document.toObject(MemberWrapper.class).getMembers();
        Member member = map.get(message.getUserId());
        member.decreaseTabs();
        if (member.getTabs() == 0) {
            member.setOnline(false);
        }
        docRef.update("members."+message.getUserId(), member);

        return null;
    }


    private Map<Integer, List<String>> getLabInfo(String roomId) throws ExecutionException, InterruptedException {
        DocumentReference docRef = db.collection("rooms").document(roomId);
        ApiFuture<DocumentSnapshot> documentSnapshot = docRef.get();
        DocumentSnapshot document = documentSnapshot.get();
        Map<String, Member> map = document.toObject(MemberWrapper.class).getMembers();

        Map<Integer, List<String>> infoMap = new HashMap<>();
        map.forEach((key, value) -> {
            int step = value.getStep();
            if (infoMap.get(step) == null) {
                infoMap.put(step, new ArrayList<>());
            }

            infoMap.get(step).add(value.getUserName());
        });

        return infoMap;
    }
}
