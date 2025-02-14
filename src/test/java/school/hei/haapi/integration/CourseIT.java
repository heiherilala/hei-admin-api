package school.hei.haapi.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;
import school.hei.haapi.SentryConf;
import school.hei.haapi.endpoint.rest.api.PayingApi;
import school.hei.haapi.endpoint.rest.api.TeachingApi;
import school.hei.haapi.endpoint.rest.api.UsersApi;
import school.hei.haapi.endpoint.rest.client.ApiClient;
import school.hei.haapi.endpoint.rest.client.ApiException;
import school.hei.haapi.endpoint.rest.model.CourseStatus;
import school.hei.haapi.endpoint.rest.model.Fee;
import school.hei.haapi.endpoint.rest.model.Student;
import school.hei.haapi.endpoint.rest.security.cognito.CognitoComponent;
import school.hei.haapi.integration.conf.TestUtils;
import school.hei.haapi.endpoint.rest.model.CrupdateCourse;
import school.hei.haapi.endpoint.rest.model.Course;
import school.hei.haapi.model.StudentCourse;

import java.util.ArrayList;
import java.util.List;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static school.hei.haapi.integration.StudentIT.student1;
import static school.hei.haapi.integration.StudentIT.student2;
import static school.hei.haapi.integration.conf.TestUtils.BAD_TOKEN;
import static school.hei.haapi.integration.conf.TestUtils.COURSE1_ID;


import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static school.hei.haapi.integration.conf.TestUtils.COURSE2_ID;
import static school.hei.haapi.integration.conf.TestUtils.COURSE3_ID;
import static school.hei.haapi.integration.conf.TestUtils.FEE1_ID;
import static school.hei.haapi.integration.conf.TestUtils.FEE2_ID;
import static school.hei.haapi.integration.conf.TestUtils.MANAGER1_TOKEN;
import static school.hei.haapi.integration.conf.TestUtils.STUDENT1_ID;
import static school.hei.haapi.integration.conf.TestUtils.STUDENT1_TOKEN;
import static school.hei.haapi.integration.conf.TestUtils.STUDENT2_ID;
import static school.hei.haapi.integration.conf.TestUtils.TEACHER1_TOKEN;
import static school.hei.haapi.integration.conf.TestUtils.assertThrowsApiException;
import static school.hei.haapi.integration.conf.TestUtils.assertThrowsForbiddenException;
import static school.hei.haapi.integration.conf.TestUtils.isValidUUID;
import static school.hei.haapi.integration.conf.TestUtils.setUpCognito;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
@ContextConfiguration(initializers = GroupIT.ContextInitializer.class)
@AutoConfigureMockMvc
public class CourseIT {

    private static String oppositeCase(String str) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (Character.isLowerCase(c)) {
                result.append(Character.toUpperCase(c));
            } else if (Character.isUpperCase(c)) {
                result.append(Character.toLowerCase(c));
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }
    @MockBean
    private SentryConf sentryConf;
    @MockBean
    private CognitoComponent cognitoComponentMock;
    private static ApiClient anApiClient(String token) {
        return TestUtils.anApiClient(token, GroupIT.ContextInitializer.SERVER_PORT);
    }
    public static Course course1(){
        Course course = new Course();
        course.setId(COURSE1_ID);
        course.setCode("PROG1");
        course.setName("Algorithmics");
        course.setCredits(3);
        course.setTotalHours(40);
        course.setMainTeacher(TeacherIT.teacher1());
        return course;
    }
    public static Course course2(){
        Course course = new Course();
        course.setId(COURSE2_ID);
        course.setCode("WEB1");
        course.setName(null);
        course.setCredits(2);
        course.setTotalHours(36);
        course.setMainTeacher(TeacherIT.teacher1());
        return course;
    }
    public static Course course3(){
        Course course = new Course();
        course.setId(COURSE3_ID);
        course.setCode("MGT1");
        course.setName("name");
        course.setCredits(2);
        course.setTotalHours(20);
        course.setMainTeacher(TeacherIT.teacher2());
        return course;
    }
    public static CrupdateCourse someCreatableCourse() {
        CrupdateCourse createCourse = new CrupdateCourse();
        createCourse.setName("Some name");
        createCourse.setCode("Code" + randomUUID());
        createCourse.setCredits(5);
        createCourse.setTotalHours(40);
        createCourse.setMainTeacherId("teacher1_id");
        return createCourse;
    }
    @BeforeEach
    void setUp() {
        setUpCognito(cognitoComponentMock);
    }
    @Test
    void manager_read_student_courses_ok() throws ApiException {
        ApiClient manager1Client = anApiClient(MANAGER1_TOKEN);
        UsersApi api = new UsersApi(manager1Client);

        List<Course> expectCours = new ArrayList<>();
        expectCours.add(0,course1());
        List<Course> actualCours = api.getStudentCoursesById(STUDENT1_ID, CourseStatus.LINKED);
        assertEquals(actualCours, expectCours);
    }
    @Test
    void read_student_courses_unliked_ok() throws ApiException {
        ApiClient manager1Client = anApiClient(MANAGER1_TOKEN);
        UsersApi api = new UsersApi(manager1Client);

        List<Course> expectCours = new ArrayList<>();
        expectCours.add(0,course2());
        List<Course> actualCours = api.getStudentCoursesById(STUDENT1_ID, CourseStatus.UNLINKED);
        assertEquals(actualCours, expectCours);
    }
    @Test
    void read_student_courses_whith_status_null_ok() throws ApiException {
        ApiClient manager1Client = anApiClient(MANAGER1_TOKEN);
        UsersApi api = new UsersApi(manager1Client);

        List<Course> expectCours = new ArrayList<>();
        expectCours.add(0,course1());
        List<Course> actualCours = api.getStudentCoursesById(STUDENT1_ID, null);
        assertEquals(actualCours, expectCours);
    }
    @Test
    void teacher_read_student_courses_ko() throws ApiException {
        ApiClient teacher1Client = anApiClient(TEACHER1_TOKEN);
        UsersApi api = new UsersApi(teacher1Client);

        assertThrowsApiException(
                "{\"type\":\"403 FORBIDDEN\",\"message\":\"Access is denied\"}",
                () -> api.getStudentCoursesById(STUDENT1_ID, CourseStatus.LINKED));
        assertThrowsApiException(
                "{\"type\":\"403 FORBIDDEN\",\"message\":\"Access is denied\"}",
                () -> api.getStudentCoursesById(STUDENT1_ID, null));
        assertThrowsApiException(
                "{\"type\":\"403 FORBIDDEN\",\"message\":\"Access is denied\"}",
                () -> api.getStudentCoursesById(STUDENT1_ID, CourseStatus.UNLINKED));
    }

    @Test
    void student_read_courses_ko() throws ApiException {
        ApiClient student1Client = anApiClient(STUDENT1_TOKEN);
        UsersApi api = new UsersApi(student1Client);

        assertThrowsApiException(
                "{\"type\":\"403 FORBIDDEN\",\"message\":\"Access is denied\"}",
                () -> api.getStudentCoursesById(STUDENT2_ID, CourseStatus.LINKED));
        assertThrowsApiException(
                "{\"type\":\"403 FORBIDDEN\",\"message\":\"Access is denied\"}",
                () -> api.getStudentCoursesById(STUDENT2_ID, null));
        assertThrowsApiException(
                "{\"type\":\"403 FORBIDDEN\",\"message\":\"Access is denied\"}",
                () -> api.getStudentCoursesById(STUDENT2_ID, CourseStatus.UNLINKED));
    }

    @Test
    void badtoken_write_ko() {
        ApiClient anonymousClient = anApiClient(BAD_TOKEN);

        TeachingApi api = new TeachingApi(anonymousClient);
        assertThrowsForbiddenException(() -> api.crupdateCourses(List.of()));
    }
    @Test
    void student_write_ko() {
        ApiClient student1Client = anApiClient(STUDENT1_TOKEN);

        TeachingApi api = new TeachingApi(student1Client);
        assertThrowsForbiddenException(() -> api.crupdateCourses(List.of()));
    }
    @Test
    void teacher_write_ko() {
        ApiClient teacher1Client = anApiClient(TEACHER1_TOKEN);

        TeachingApi api = new TeachingApi(teacher1Client);
        assertThrowsForbiddenException(() -> api.crupdateCourses(List.of()));
    }
    /*
    @Test
    void manager_write_create_ok() throws ApiException {
        ApiClient manager1Client = anApiClient(MANAGER1_TOKEN);
        CrupdateCourse toCreate3 = someCreatableCourse();
        CrupdateCourse toCreate4 = someCreatableCourse();

        TeachingApi api = new TeachingApi(manager1Client);
        List<Course> created = api.crupdateCourses(List.of(toCreate3, toCreate4));

        assertEquals(2, created.size());
        Course created3 = created.get(0);
        assertTrue(isValidUUID(created3.getId()));
        toCreate3.setId(created3.getId());
        //
        assertEquals(created3, toCreate3);
        Course created4 = created.get(0);
        assertTrue(isValidUUID(created4.getId()));
        toCreate4.setId(created4.getId());
        assertEquals(created4, toCreate3);
    }
    @Test
    void manager_write_update_ok() throws ApiException {
        ApiClient manager1Client = anApiClient(MANAGER1_TOKEN);
        TeachingApi api = new TeachingApi(manager1Client);
        List<Course> toUpdate = api.crupdateCourses(List.of(
                someCreatableCourse(),
                someCreatableCourse()));
        Course toUpdate0 = toUpdate.get(0);
        toUpdate0.setName("A new name zero");
        Course toUpdate1 = toUpdate.get(1);
        toUpdate1.setName("A new name one");
        assertEquals(2, toUpdate.size());
        assertTrue(toUpdate.contains(toUpdate0));
        assertTrue(toUpdate.contains(toUpdate1));
    }
     */
    @Test
    void bad_token_read_ko() {
        ApiClient anonymousClient = anApiClient(BAD_TOKEN);
        TeachingApi api = new TeachingApi(anonymousClient);
        assertThrowsApiException(
                "{\"type\":\"403 FORBIDDEN\",\"message\":\"Access is denied\"}",
                () -> api.getCourses(null,null,"","","","","","",""));
    }
    @Test
    void teacher_read_ok() throws school.hei.haapi.endpoint.rest.client.ApiException {
        ApiClient teacher1Client = anApiClient(TEACHER1_TOKEN);

        TeachingApi api = new TeachingApi(teacher1Client);
        List<Course> actualCourses = api.getCourses(null,null,"","","","","","","");

        assertTrue(actualCourses.contains(course1()));
        assertTrue(actualCourses.contains(course2()));
        assertTrue(actualCourses.contains(course3()));
    }
    @Test
    void manager_read_ok() throws school.hei.haapi.endpoint.rest.client.ApiException {
        ApiClient manager1Client = anApiClient(MANAGER1_TOKEN);
        TeachingApi api = new TeachingApi(manager1Client);
        List<Course> actualCourses = api.getCourses(null,null,"","","","","","","");

        assertTrue(actualCourses.contains(course1()));
        assertTrue(actualCourses.contains(course2()));
        assertTrue(actualCourses.contains(course3()));
    }
    @Test
    void student_read_ok() throws school.hei.haapi.endpoint.rest.client.ApiException {
        ApiClient student1Client = anApiClient(STUDENT1_TOKEN);
        TeachingApi api = new TeachingApi(student1Client);
        List<Course> actualCourses = api.getCourses(null,null,"","","","","","","");

        assertTrue(actualCourses.contains(course1()));
        assertTrue(actualCourses.contains(course2()));
        assertTrue(actualCourses.contains(course3()));
    }
    @Test
    void reading_with_teacher_first_name_part_filter_case_insensitive_ok() throws school.hei.haapi.endpoint.rest.client.ApiException {
        ApiClient manager1Client = anApiClient(MANAGER1_TOKEN);
        TeachingApi api = new TeachingApi(manager1Client);
        List<Course> actualCourses = api.getCourses(null,null,"","","",oppositeCase(course1().getMainTeacher().getFirstName().substring(0, 2)),"","","");

        assertTrue(actualCourses.contains(course1()));
    }
    @Test
    void reading_with_teacher_last_name_part_filter_case_insensitive_ok() throws school.hei.haapi.endpoint.rest.client.ApiException {
        ApiClient manager1Client = anApiClient(MANAGER1_TOKEN);
        TeachingApi api = new TeachingApi(manager1Client);
        List<Course> actualCourses = api.getCourses(null,null,"","","","",oppositeCase(course1().getMainTeacher().getLastName().substring(0, 2)),"","");

        assertTrue(actualCourses.contains(course1()));
    }
    @Test
    void reading_with_teacher_last_name_and_first_name_filter_take_all_of_items_of_them_ok() throws school.hei.haapi.endpoint.rest.client.ApiException {
        ApiClient manager1Client = anApiClient(MANAGER1_TOKEN);
        TeachingApi api = new TeachingApi(manager1Client);
        List<Course> actualCourses = api.getCourses(null,null,"","","",oppositeCase(course2().getMainTeacher().getFirstName().substring(0, 2)),oppositeCase(course1().getMainTeacher().getLastName().substring(0, 2)),"","");

        assertTrue(actualCourses.contains(course1()));
        assertTrue(actualCourses.contains(course2()));
    }
    @Test
    void reading_with_name_part_filter_case_insensitive_ok() throws school.hei.haapi.endpoint.rest.client.ApiException {
        ApiClient manager1Client = anApiClient(MANAGER1_TOKEN);
        TeachingApi api = new TeachingApi(manager1Client);
        List<Course> actualCourses = api.getCourses(null,null,"",oppositeCase(course1().getName().substring(0, 2)),"","","","","");

        assertTrue(actualCourses.contains(course1()));
    }
    @Test
    void reading_with_code_part_filter_case_insensitive_ok() throws school.hei.haapi.endpoint.rest.client.ApiException {
        ApiClient manager1Client = anApiClient(MANAGER1_TOKEN);
        TeachingApi api = new TeachingApi(manager1Client);
        List<Course> actualCourses = api.getCourses(null,null,oppositeCase(course1().getCode().substring(0, 2)),"","","","","","");

        assertTrue(actualCourses.contains(course1()));
    }
    @Test
    void reading_with_exact_credits_filter_ok() throws school.hei.haapi.endpoint.rest.client.ApiException {
        ApiClient manager1Client = anApiClient(MANAGER1_TOKEN);
        TeachingApi api = new TeachingApi(manager1Client);
        List<Course> actualCourses = api.getCourses(null,null,"","",course1().getCredits().toString(),"","","","");

        assertTrue(actualCourses.contains(course1()));
    }
    @Test
    void reading_with_not_exact_credits_filter_ko() throws school.hei.haapi.endpoint.rest.client.ApiException {
        ApiClient manager1Client = anApiClient(MANAGER1_TOKEN);
        TeachingApi api = new TeachingApi(manager1Client);
        List<Course> actualCourses = api.getCourses(null,null,"", "",course1().getCredits().toString().concat("1"),"","","","");

        assertFalse(actualCourses.contains(course1()));
    }
    @Test
    void reading_in_credits_order_asc_ok() throws school.hei.haapi.endpoint.rest.client.ApiException {
        ApiClient manager1Client = anApiClient(MANAGER1_TOKEN);
        TeachingApi api = new TeachingApi(manager1Client);
        List<Course> actualCourses = api.getCourses(null,null,"","","","","","ASC","");

        assertTrue(actualCourses.contains(course1()));
        assertTrue(actualCourses.contains(course2()));
        assertTrue(actualCourses.contains(course3()));
        assertTrue(actualCourses.get(0).getCredits()<=actualCourses.get(1).getCredits());
        assertTrue(actualCourses.get(1).getCredits()<=actualCourses.get(2).getCredits());
    }
    @Test
    void reading_in_credits_order_desc_ok() throws school.hei.haapi.endpoint.rest.client.ApiException {
        ApiClient manager1Client = anApiClient(MANAGER1_TOKEN);
        TeachingApi api = new TeachingApi(manager1Client);
        List<Course> actualCourses = api.getCourses(null,null,"","","","","","DESC","");

        assertTrue(actualCourses.contains(course1()));
        assertTrue(actualCourses.contains(course2()));
        assertTrue(actualCourses.contains(course3()));
        assertTrue(actualCourses.get(0).getCredits()>=actualCourses.get(1).getCredits());
        assertTrue(actualCourses.get(1).getCredits()>=actualCourses.get(2).getCredits());
    }
    @Test
    void reading_in_code_order_asc_ok() throws school.hei.haapi.endpoint.rest.client.ApiException {
        ApiClient manager1Client = anApiClient(MANAGER1_TOKEN);
        TeachingApi api = new TeachingApi(manager1Client);
        List<Course> actualCourses = api.getCourses(null,null,"","","","","","","ASC");

        assertTrue(actualCourses.contains(course1()));
        assertTrue(actualCourses.contains(course2()));
        assertTrue(actualCourses.contains(course3()));
        assertTrue((actualCourses.get(0).getCode().compareTo(actualCourses.get(1).getCode()))<=0);
        assertTrue((actualCourses.get(1).getCode().compareTo(actualCourses.get(2).getCode()))<=0);
    }
    @Test
    void reading_in_code_order_desc_ok() throws school.hei.haapi.endpoint.rest.client.ApiException {
        ApiClient manager1Client = anApiClient(MANAGER1_TOKEN);
        TeachingApi api = new TeachingApi(manager1Client);
        List<Course> actualCourses = api.getCourses(null,null,"","","","","","","DESC");

        assertTrue(actualCourses.contains(course1()));
        assertTrue(actualCourses.contains(course2()));
        assertTrue(actualCourses.contains(course3()));
        assertTrue((actualCourses.get(0).getCode().compareTo(actualCourses.get(1).getCode()))>=0);
        assertTrue((actualCourses.get(1).getCode().compareTo(actualCourses.get(2).getCode()))>=0);
    }
    @Test
    void reading_in_credits_order_with_bad_parameters_ko() throws school.hei.haapi.endpoint.rest.client.ApiException {
        ApiClient manager1Client = anApiClient(MANAGER1_TOKEN);
        TeachingApi api = new TeachingApi(manager1Client);
        assertThrowsApiException(
                "{\"type\":\"400 BAD_REQUEST\",\"message\":\"credits parameter is different of ASC and DESC\"}",
                () -> api.getCourses(null,null,"","","","","","BAD",""));
    }
    @Test
    void reading_in_code_order_with_bad_parameters_ko() throws school.hei.haapi.endpoint.rest.client.ApiException {
        ApiClient manager1Client = anApiClient(MANAGER1_TOKEN);
        TeachingApi api = new TeachingApi(manager1Client);
        assertThrowsApiException(
                "{\"type\":\"400 BAD_REQUEST\",\"message\":\"code parameter is different of ASC and DESC\"}",
                () -> api.getCourses(null,null,"","","","","","","BAD"));
    }
}
