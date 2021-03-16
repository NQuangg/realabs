package com.vi.realabs.controller;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;
import com.google.gson.Gson;
import com.vi.realabs.model.firestore.Classroom;
import com.vi.realabs.model.firestore.Lab;
import com.vi.realabs.model.LabId;
import com.vi.realabs.model.LabFile;
import com.vi.realabs.model.classroom.CourseWrapper;
import com.vi.realabs.model.UserInfo;
import com.vi.realabs.model.firestore.User;
import com.vi.realabs.model.classroom.Student;
import com.vi.realabs.model.classroom.StudentWrapper;
import com.vi.realabs.model.classroom.Teacher;
import com.vi.realabs.model.classroom.TeacherWrapper;
import com.vi.realabs.utils.FileUtil;
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
import java.util.concurrent.ExecutionException;

@Controller
@RequiredArgsConstructor
public class WebController {
    private final OAuth2AuthorizedClientService authorizedClientService;

    @Autowired
    private ClientRegistrationRepository clientRegistrationRepository;

    Firestore db = FirestoreClient.getFirestore();

    @GetMapping("/")
    public String getMain(Model model, OAuth2AuthenticationToken token) throws ExecutionException, InterruptedException {
        if (token != null) {
            UserInfo userInfo = callApiUserInfo(token);
            model.addAttribute("userInfo", userInfo);

            DocumentReference docRef = db.collection("users").document(userInfo.getSub());
            ApiFuture<DocumentSnapshot> documentSnapshot = docRef.get();
            DocumentSnapshot document = documentSnapshot.get();
            if (!document.exists()) {
                User user = new User(userInfo.getName(), userInfo.getEmail(), Arrays.asList());
                db.collection("users").document(userInfo.getSub()).set(user);
            }
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

    @GetMapping("/my-labs")
    public String getLabs(Model model, OAuth2AuthenticationToken token) throws IOException, ExecutionException, InterruptedException {
        UserInfo userInfo = callApiUserInfo(token);
        model.addAttribute("userInfo", userInfo);
        model.addAttribute("labId", new LabId());

        DocumentReference docRef = db.collection("users").document(userInfo.getSub());
        ApiFuture<DocumentSnapshot> documentSnapshot = docRef.get();
        List<Lab> labs = documentSnapshot.get().toObject(User.class).getLabs();
        model.addAttribute("labs", labs);

        return "my-labs";
    }

    @PostMapping("/my-labs")
    public String postLabs(@ModelAttribute LabId labId, Model model, OAuth2AuthenticationToken token) throws IOException, ExecutionException, InterruptedException {
        String userId = callApiUserInfo(token).getSub();
        createCodelab(labId.getId(), userId);

        String data = FileUtil.readFile(userId+labId.getId(), false);
        LabFile labFile = new Gson().fromJson(data, LabFile.class);

        Lab lab = new Lab(userId+ labId.getId(), labFile.getTitle());
        DocumentReference docRef = db.collection("users").document(userId);
        docRef.update("labs", FieldValue.arrayUnion(lab));

        return getLabs(model, token);
    }

    @GetMapping("/my-labs/delete")
    public String deleteLabs(Model model, OAuth2AuthenticationToken token, @RequestParam(name = "id") String labId) throws IOException, ExecutionException, InterruptedException {
        UserInfo userInfo = callApiUserInfo(token);

        DocumentReference docRef = db.collection("users").document(userInfo.getSub());
        ApiFuture<DocumentSnapshot> documentSnapshot = docRef.get();
        List<Lab> userlabs = documentSnapshot.get().toObject(Classroom.class).getLabs();
        for (Lab lab: userlabs) {
            if (lab.getId().equals(labId)) {
                docRef.update("labs", FieldValue.arrayRemove(lab));
            }
        }

        FileUtil.deleteFile(new File(labId));

        ApiFuture<QuerySnapshot> querySnapshot = db.collection("classrooms").get();
        List<QueryDocumentSnapshot> documents = querySnapshot.get().getDocuments();
        for (QueryDocumentSnapshot document : documents) {
            Classroom classroom = document.toObject(Classroom.class);
            if (classroom.getTeacherId().equals(userInfo.getSub())) {
                List<Lab> classroomLabs = classroom.getLabs();
                for (Lab lab: classroomLabs) {
                    if (lab.getId().equals(labId)) {
                        docRef = db.collection("classrooms").document(document.getId());
                        docRef.update("labs", FieldValue.arrayRemove(lab));
                    }
                }
            }
        }

        return "redirect:/my-labs";
    }

    @GetMapping("/lab")
    public String getLab(@RequestParam(name = "id") String labId, @RequestParam String classroomId, Model model, OAuth2AuthenticationToken token) throws IOException {
        UserInfo userInfo = callApiUserInfo(token);
        if (!isTeacher(token, classroomId, userInfo.getSub()) && !isStudent(token, classroomId, userInfo.getSub())) {
            return "404";
        }

        File inputFile = new File(labId+"/index.html");
        Document doc = Jsoup.parse(inputFile, "UTF-8");
        model.addAttribute("html", doc.toString());

        model.addAttribute("userInfo", userInfo);
        model.addAttribute("labId", labId);

        return "lab";
    }

    @GetMapping("/preview-lab")
    public String getPreviewLab(@RequestParam(name = "id") String labId, Model model, OAuth2AuthenticationToken token) throws IOException {
        File inputFile = new File(labId+"/index.html");
        Document doc = Jsoup.parse(inputFile, "UTF-8");
        model.addAttribute("html", doc.toString());

        return "preview-lab";
    }

    @GetMapping("/document")
    public String getDocumentation(Model model, OAuth2AuthenticationToken token) {
        if (token != null) {
            UserInfo userInfo = callApiUserInfo(token);
            model.addAttribute("userInfo", userInfo);
        }

        return "document";
    }

    @GetMapping("/student")
    public String getStudentCourse(Model model, OAuth2AuthenticationToken token) {
        CourseWrapper courseWrapper = callApiCourse(token, URI.create("https://classroom.googleapis.com/v1/courses?studentId=me"));
        model.addAttribute("courses", courseWrapper.getCourses());

        UserInfo userInfo = callApiUserInfo(token);
        model.addAttribute("userInfo", userInfo);

        return "student";
    }

    @GetMapping("/student/classrooms/{classroomId}")
    public String getStudentClassroom(Model model, OAuth2AuthenticationToken token, @PathVariable(name = "classroomId") String classroomId) throws ExecutionException, InterruptedException {
        UserInfo userInfo = callApiUserInfo(token);

        if (!isStudent(token, classroomId, userInfo.getSub())) {
            return "404";
        }

        model.addAttribute("userInfo", userInfo);
        model.addAttribute("classroomId", classroomId);

        DocumentReference docRef = db.collection("classrooms").document(classroomId);
        ApiFuture<DocumentSnapshot> documentSnapshot = docRef.get();
        DocumentSnapshot document = documentSnapshot.get();

        List<Lab> labs = new ArrayList<>();

        if (document.exists()) {
            labs = document.toObject(Classroom.class).getLabs();
        }

        model.addAttribute("labs", labs);

        return "student-classroom";
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
    public String getTeacherClassroom(Model model, OAuth2AuthenticationToken token, @PathVariable(name = "classroomId") String classroomId) throws ExecutionException, InterruptedException {
        UserInfo userInfo = callApiUserInfo(token);

        if (!isTeacher(token, classroomId, userInfo.getSub())) {
            return "404";
        }

        model.addAttribute("userInfo", userInfo);
        model.addAttribute("classroomId", classroomId);

        DocumentReference docRef = db.collection("users").document(userInfo.getSub());
        ApiFuture<DocumentSnapshot> documentSnapshot = docRef.get();
        List<Lab> mylabs = documentSnapshot.get().toObject(User.class).getLabs();

        docRef = db.collection("classrooms").document(classroomId);
        documentSnapshot = docRef.get();
        DocumentSnapshot document = documentSnapshot.get();

        List<Lab> havedLabs = new ArrayList<>();
        List<Lab> currentLabs = new ArrayList<>();

        if (!document.exists()) {
            Classroom classroom = new Classroom(userInfo.getSub(), Arrays.asList());
            docRef.set(classroom);
            currentLabs = mylabs;
        } else {
            if (!document.toObject(Classroom.class).getLabs().isEmpty()) {
                havedLabs = document.toObject(Classroom.class).getLabs();
                label:
                for (Lab mylab: mylabs) {
                    for (Lab havedLab : havedLabs) {
                        if (mylab.getId().equals(havedLab.getId())) {
                            continue label;
                        }
                    }
                    currentLabs.add(mylab);
                }
            } else {
                currentLabs = mylabs;
            }
        }
        model.addAttribute("currentLabs", currentLabs);
        model.addAttribute("havedLabs", havedLabs);
        model.addAttribute("labId", new LabId());

        return "teacher-classroom";
    }

    @PostMapping("/teacher/classrooms/{classroomId}")
    public String postTeacherClassroom(@ModelAttribute LabId labId, Model model, OAuth2AuthenticationToken token, @PathVariable(name = "classroomId") String classroomId) throws ExecutionException, InterruptedException {
        UserInfo userInfo = callApiUserInfo(token);

        DocumentReference docRef = db.collection("users").document(userInfo.getSub());
        ApiFuture<DocumentSnapshot> documentSnapshot = docRef.get();
        List<Lab> labList = documentSnapshot.get().toObject(User.class).getLabs();

        Lab lab = new Lab();
        for (Lab item: labList) {
            if (item.getId().equals(labId.getId())) {
                lab = item;
                break;
            }
        }

        docRef = db.collection("classrooms").document(classroomId);
        docRef.update("labs", FieldValue.arrayUnion(lab));

        return getTeacherClassroom(model, token, classroomId);
    }

    @GetMapping("/teacher/classrooms/{classroomId}/delete")
    public String deleteTeacherClassroom(Model model, OAuth2AuthenticationToken token, @PathVariable(name = "classroomId") String classroomId, @RequestParam(name = "id") String labId) throws ExecutionException, InterruptedException {
        UserInfo userInfo = callApiUserInfo(token);

        if (!isTeacher(token, classroomId, userInfo.getSub())) {
            return "404";
        }

        DocumentReference docRef = db.collection("classrooms").document(classroomId);
        ApiFuture<DocumentSnapshot> documentSnapshot = docRef.get();
        List<Lab> labs = documentSnapshot.get().toObject(Classroom.class).getLabs();

        for (Lab lab : labs) {
            if (lab.getId().equals(labId)) {
                docRef.update("labs", FieldValue.arrayRemove(lab));
            }
        }

        return "redirect:/teacher/classrooms/"+classroomId;
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

    private void createCodelab(String labId, String userId) throws IOException {
        Runtime runtime = Runtime.getRuntime();
        Process process = runtime.exec("claat export "+labId);

        BufferedReader stdError = new BufferedReader(new InputStreamReader(process.getErrorStream()));
        StringBuilder str = new StringBuilder();
        String s = null;

        while ((s = stdError.readLine()) != null) {
            str.append(s);
        }

        String folderName = str.toString().replaceFirst("ok", "").trim();
        System.out.println(folderName);

        File sourceFile = new File(folderName);
        File targetFile = new File(userId+labId);
        boolean checkRename = sourceFile.renameTo(targetFile);
        if (checkRename) {
            System.out.println("folder YES");
        } else {
            System.out.println("folder NO");
        }
    }

    private boolean isStudent(OAuth2AuthenticationToken token, String classroomId, String userId) {
        OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(token.getAuthorizedClientRegistrationId(), token.getPrincipal().getName());
        URI uri = URI.create("https://classroom.googleapis.com/v1/courses/"+classroomId+"/students");
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, "Bearer "+client.getAccessToken().getTokenValue());
        RequestEntity<String> request = new RequestEntity<String>("", headers, HttpMethod.GET, uri);
        ResponseEntity<StudentWrapper> response = restTemplate.exchange(request, StudentWrapper.class);
        List<Student> students = response.getBody().getStudents();

        boolean isStudent = false;
        for (Student student: students) {
            if (student.getUserId().equals(userId)) {
                isStudent = true;
            }
        }
        return isStudent;
    }

    private boolean isTeacher(OAuth2AuthenticationToken token, String classroomId, String userId) {
        OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(token.getAuthorizedClientRegistrationId(), token.getPrincipal().getName());
        URI uri = URI.create("https://classroom.googleapis.com/v1/courses/"+classroomId+"/teachers");
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, "Bearer "+client.getAccessToken().getTokenValue());
        RequestEntity<String> request = new RequestEntity<String>("", headers, HttpMethod.GET, uri);
        ResponseEntity<TeacherWrapper> response = restTemplate.exchange(request, TeacherWrapper.class);
        List<Teacher> teachers = response.getBody().getTeachers();

        boolean isTeacher = false;
        for (Teacher teacher: teachers) {
            if (teacher.getUserId().equals(userId)) {
                isTeacher = true;
            }
        }
        return isTeacher;
    }

}
