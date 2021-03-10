package com.vi.realabs.controller;

import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;
import com.vi.realabs.model.classroom.Course;
import com.vi.realabs.model.classroom.CourseWrapper;
import com.vi.realabs.model.UserInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ResolvableType;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;

import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@RestController
@RequiredArgsConstructor
public class WebRestController {

    private final OAuth2AuthorizedClientService authorizedClientService;

    @Autowired
    private ClientRegistrationRepository clientRegistrationRepository;
    Firestore db = FirestoreClient.getFirestore();

//    @GetMapping("/demo")
//    public Principal get(Authentication authentication) {
//        return authentication;
//    }

    @GetMapping("/api/profile")
    public UserInfo getProfile(OAuth2AuthenticationToken token) {
        UserInfo userInfo = callApiUserInfo(token);

        return userInfo;
    }

    @GetMapping("/api/login")
    public Map<String, String> getLogin() {
        String authorizationRequestBaseUri = "oauth2/authorization";
        Map<String, String> oauth2AuthenticationUrls = new HashMap<>();
        Iterable<ClientRegistration> clientRegistrations = null;
        ResolvableType type = ResolvableType.forInstance(clientRegistrationRepository)
                .as(Iterable.class);
        if (type != ResolvableType.NONE &&
                ClientRegistration.class.isAssignableFrom(type.resolveGenerics()[0])) {
            clientRegistrations = (Iterable<ClientRegistration>) clientRegistrationRepository;
        }

        clientRegistrations.forEach(registration ->
                oauth2AuthenticationUrls.put(registration.getClientName(),
                        authorizationRequestBaseUri + "/" + registration.getRegistrationId()));

        return oauth2AuthenticationUrls;
    }

    @GetMapping("/api/labs")
    public String getLabs(Model model, OAuth2AuthenticationToken token) {
        UserInfo userInfo = callApiUserInfo(token);
        model.addAttribute("userInfo", userInfo);

        return "labs";
    }

    @GetMapping("/api/teacher")
    public List<Course> getTeacherCourse(OAuth2AuthenticationToken token) {
        CourseWrapper courseWrapper = callApiCourse(token, URI.create("https://classroom.googleapis.com/v1/courses?teacherId=me"));
        List<Course> courses = courseWrapper.getCourses();

        return courses;
    }

    @GetMapping("/api/teacher/classrooms/{classroomId}")
    public String getClassroom(Model model, OAuth2AuthenticationToken token, @PathVariable(name = "classroomId") String id) {
        UserInfo userInfo = callApiUserInfo(token);

        OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(token.getAuthorizedClientRegistrationId(), token.getPrincipal().getName());
        URI uri = URI.create("https://classroom.googleapis.com/v1/courses/"+id+"/teachers");
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, "Bearer "+client.getAccessToken().getTokenValue());
        RequestEntity<String> request = new RequestEntity<String>("", headers, HttpMethod.GET, uri);
        ResponseEntity<String> response = restTemplate.exchange(request, String.class);


        return response.getBody();
    }

    @GetMapping("/api/student")
    public List<Course> getStudentCourse(OAuth2AuthenticationToken token) {
        CourseWrapper courseWrapper = callApiCourse(token, URI.create("https://classroom.googleapis.com/v1/courses?studentId=me"));
        List<Course> courses = courseWrapper.getCourses();

        return courses;
    }

    private UserInfo callApiUserInfo(OAuth2AuthenticationToken token) {
        OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(token.getAuthorizedClientRegistrationId(), token.getPrincipal().getName());
        URI uri = URI.create(client.getClientRegistration().getProviderDetails().getUserInfoEndpoint().getUri());
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, "Bearer "+client.getAccessToken().getTokenValue());
        RequestEntity<String> request = new RequestEntity<String>("", headers, HttpMethod.GET, uri);
        ResponseEntity<UserInfo> response = restTemplate.exchange(request, UserInfo.class);

        return response.getBody();
    }

    private CourseWrapper callApiCourse(OAuth2AuthenticationToken token, URI uri) {
        OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(token.getAuthorizedClientRegistrationId(), token.getPrincipal().getName());
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, "Bearer "+client.getAccessToken().getTokenValue());
        RequestEntity<String> request = new RequestEntity<String>("", headers, HttpMethod.GET, uri);
        ResponseEntity<CourseWrapper> response = restTemplate.exchange(request, CourseWrapper.class);

        return response.getBody();
    }

//    @GetMapping("/api")
//    public String get(OAuth2AuthenticationToken token) throws ExecutionException, InterruptedException {
//        UserInfo userInfo = callApiUserInfo(token);
//
//        ApiFuture<QuerySnapshot> querySnapshot = db.collection("classrooms").get();
//        List<QueryDocumentSnapshot> documents = querySnapshot.get().getDocuments();
//        for (QueryDocumentSnapshot document : documents) {
//            String a = document.getId();
//            System.out.println(a);
//        }
//
//        return "hi";
//    }
}
