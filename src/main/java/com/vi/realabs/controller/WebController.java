package com.vi.realabs.controller;

import com.vi.realabs.model.CourseWrapper;
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
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class WebController {
    private final OAuth2AuthorizedClientService authorizedClientService;

    @Autowired
    private ClientRegistrationRepository clientRegistrationRepository;

    @GetMapping("/")
    public String getMain(Model model, OAuth2AuthenticationToken token) {
        if (token != null) {
            UserInfo userInfo = callApiUserInfo(token);
            model.addAttribute("userInfo", userInfo);
        }

        return "index";
    }

    @GetMapping("/login")
    public String getLogin(Model model) {
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
        model.addAttribute("urls", oauth2AuthenticationUrls);

        return "login";
    }

    @GetMapping("/labs")
    public String getLabs(Model model, OAuth2AuthenticationToken token) {
        UserInfo userInfo = callApiUserInfo(token);
        model.addAttribute("userInfo", userInfo);

        return "labs";
    }

    @GetMapping("/document")
    public String getDocumentation(Model model, OAuth2AuthenticationToken token) {
        if (token != null) {
            UserInfo userInfo = callApiUserInfo(token);
            model.addAttribute("userInfo", userInfo);
        }

        return "document";
    }

    @GetMapping("/teacher")
    public String getTeacherCourse(Model model, OAuth2AuthenticationToken token) {
        CourseWrapper courseWrapper = callApiCourse(token, URI.create("https://classroom.googleapis.com/v1/courses?teacherId=me"));
        model.addAttribute("courses", courseWrapper.getCourses());

        UserInfo userInfo = callApiUserInfo(token);
        model.addAttribute("userInfo", userInfo);

        return "teacher";
    }

    @GetMapping("/teacher/classrooms/{classroomId}")
    public String getClassroom(Model model, OAuth2AuthenticationToken token, @PathVariable(name = "classroomId") String id) {
        UserInfo userInfo = callApiUserInfo(token);
        model.addAttribute("userInfo", userInfo);

        model.addAttribute("id", id);
        return "classroom";
    }

    @GetMapping("/student")
    public String getStudentCourse(Model model, OAuth2AuthenticationToken token) {
        CourseWrapper courseWrapper = callApiCourse(token, URI.create("https://classroom.googleapis.com/v1/courses?studentId=me"));
        model.addAttribute("courses", courseWrapper.getCourses());

        UserInfo userInfo = callApiUserInfo(token);
        model.addAttribute("userInfo", userInfo);

        return "student";
    }

    @GetMapping("/profile")
    public String getProfile(Model model, OAuth2AuthenticationToken token) {
        UserInfo userInfo = callApiUserInfo(token);
        model.addAttribute("userInfo", userInfo);

        return "profile";
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

}
