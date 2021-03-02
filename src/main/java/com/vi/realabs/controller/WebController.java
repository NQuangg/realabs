package com.vi.realabs.controller;

import com.google.gson.Gson;
import com.vi.realabs.model.Codelab;
import com.vi.realabs.model.CodelabData;
import com.vi.realabs.model.CourseWrapper;
import com.vi.realabs.model.UserInfo;
import com.vi.realabs.script.FileWork;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.*;

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

    @GetMapping("/mylabs")
    public String getLabs(Model model, OAuth2AuthenticationToken token) throws IOException {
        UserInfo userInfo = callApiUserInfo(token);
        model.addAttribute("userInfo", userInfo);
        model.addAttribute("codelab", new Codelab());
        model.addAttribute("list", FileWork.readCodelabFile());

        return "mylabs";
    }

    @PostMapping("/mylabs")
    public String postLabs(@ModelAttribute Codelab codelab, Model model, OAuth2AuthenticationToken token) throws IOException {
        createCodelab(codelab.getId());
        String data = FileWork.readFile(codelab.getId(), false);
        CodelabData codelabData = new Gson().fromJson(data, CodelabData.class);

        List<CodelabData> codelabDatas = FileWork.readCodelabFile();
        if (codelabDatas == null) {
            codelabDatas = new ArrayList<>();
        }
        codelabDatas.add(codelabData);

        FileWork.writeCodelabFile(new Gson().toJson(codelabDatas));

        return getLabs(model, token);
    }

    @GetMapping("/lab")
    public String get(@RequestParam(name = "id") String labId, Model model, OAuth2AuthenticationToken token) throws IOException {
        File input = new File(labId+"/index.html");
        Document doc = Jsoup.parse(input, "UTF-8");
        model.addAttribute("labId", labId);
        model.addAttribute("html", doc.toString());

        UserInfo userInfo = callApiUserInfo(token);
        String userId = Base64.getEncoder().encodeToString(userInfo.getEmail().getBytes());
        userInfo.setId(userId);
        model.addAttribute("userInfo", userInfo);

        return "lab";
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

    private void createCodelab(String id) throws IOException {
        Runtime runtime = Runtime.getRuntime();
        Process process = runtime.exec("claat export "+id);

        BufferedReader stdError = new BufferedReader(new InputStreamReader(process.getErrorStream()));
        StringBuilder str = new StringBuilder();
        String s = null;

        while ((s = stdError.readLine()) != null) {
            str.append(s);
        }

        String folderName = str.toString().replace("ok", "").trim();
        System.out.println(folderName);

        File sourceFile = new File(folderName);
        File targetFile = new File(id);
        boolean checkRename = sourceFile.renameTo(targetFile);
        if (checkRename) {
            System.out.println("folder YES");
        } else {
            System.out.println("folder NO");
        }
    }
}
