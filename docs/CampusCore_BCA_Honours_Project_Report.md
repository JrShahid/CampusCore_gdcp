# CampusCore

## BCA Honours Mini Project Report / Dissertation

**Project Title:** CampusCore  
**Institution:** Government Degree College Pulwama (Boys)  
**Short Institutional Name:** GDC Pulwama  
**Project Type:** Final Semester BCA Honours Mini Project  
**Application Type:** Android Academic Management Platform  
**Version Documented:** 1.0 MVP implementation observed in the project source tree  

---

# Certificate

This is to certify that the mini project report entitled **CampusCore** is based on an Android application developed for **Government Degree College Pulwama (Boys)**. The project implements an academic management platform using Java, Android XML layouts, Firebase Authentication, Cloud Firestore, Cloudinary, OkHttp, Glide, and role-based workflows for administrators, teachers, and students.

The report documents the implementation observed in the actual source code, resources, Gradle configuration, Firebase security rules, activities, fragments, repositories, models, adapters, and utility classes of the CampusCore Android project.

---

# Declaration

I declare that this report describes the implemented CampusCore Android application and is prepared as part of the BCA Honours final semester mini project work. The analysis and documentation are derived from the actual project implementation, including Java classes, XML resources, Gradle files, Firebase integration, Firestore rules, repositories, models, and user interface modules.

---

# Acknowledgement

I express my sincere gratitude to the faculty and project supervisor of **Government Degree College Pulwama (Boys)** for guidance and academic support during the development of CampusCore. I also acknowledge the open-source Android ecosystem and Firebase platform, which provided the technical foundation for implementing authentication, database access, cloud-backed academic workflows, and mobile user interfaces.

---

# Abstract

CampusCore is an Android-based academic management platform developed for Government Degree College Pulwama (Boys). The system centralizes common institutional workflows such as student onboarding, teacher onboarding, academic structure management, teaching assignment management, attendance submission, attendance history, notes upload, notes access, and academic updates. The application is implemented in Java using Android activities, fragments, XML layouts, Material UI components, ViewBinding, Firebase Authentication, Cloud Firestore, Cloudinary raw upload, OkHttp, and Glide.

The system follows a role-based design. Administrators maintain departments, subjects, students, pending teacher records, and teaching assignments. Teachers access assigned classes, mark attendance for assignment-scoped class groups, upload PDF notes to Cloudinary, and view their attendance and note history. Students access their attendance records, academic notes for their department and semester, profile information, and academic or technology updates from RSS feeds. Firestore security rules enforce role-based access control and assignment-scoped writes so that teachers can write attendance and notes only for valid active teaching assignments.

CampusCore models institutional workflows through explicit Firestore collections such as `users`, `pendingStudents`, `pendingTeachers`, `departments`, `subjects`, `teachingAssignments`, `attendanceSessions`, `attendanceRecords`, and `notes`. The system uses repositories as data-access boundaries and applies validation, network checks, readable error handling, and server timestamps to support reliable academic operations. This report documents the observed implementation, architecture, data model, workflows, security design, testing scope, diagram specifications, viva preparation, and presentation outline.

---

# Table of Contents

1. Introduction  
2. Problem Statement  
3. Objectives  
4. Literature Review  
5. Requirement Analysis  
6. Project Analysis  
7. System Architecture  
8. Database Design  
9. Implementation  
10. Module Documentation  
11. Security Architecture  
12. Testing and Validation  
13. Results and Discussion  
14. Conclusion  
15. Future Scope  
16. Diagram Specifications  
17. Viva Preparation  
18. Presentation Outline  
19. References  
20. Appendices  

---

# List of Figures

Figure 1. Use Case Diagram Specification  
Figure 2. DFD Level 0 Specification  
Figure 3. DFD Level 1 Specification  
Figure 4. System Architecture Diagram Specification  
Figure 5. Firestore Relationship Diagram Specification  
Figure 6. Module Interaction Diagram Specification  

---

# List of Tables

Table 1. User Roles and Responsibilities  
Table 2. Technical Stack  
Table 3. Firestore Collections  
Table 4. Repository Responsibilities  
Table 5. Functional Testing Matrix  
Table 6. Security Validation Summary  
Table 7. Module Validation Summary  

---

# Chapter 1: Introduction

## 1.1 Overview

CampusCore is an Android application designed as an academic management ecosystem for **Government Degree College Pulwama (Boys)**. The application brings together administrative, teaching, and student-facing academic workflows into a single mobile platform. It is not a generic campus app; it is implemented around college-specific academic operations such as department management, subject management, student records, teacher records, class assignments, attendance marking, notes sharing, and academic updates.

The project is implemented in Java using Android native components. The user interface is built with XML layouts, Material components, RecyclerViews, DrawerLayout, BottomNavigationView, fragments, adapters, and ViewBinding. Firebase Authentication is used for login, signup, email verification, password reset, and session identity. Cloud Firestore acts as the operational database for academic records. Cloudinary is used for PDF note storage, while Firestore stores note metadata and secure PDF URLs. OkHttp supports both Cloudinary upload and RSS feed retrieval. Glide is used for image loading in update cards.

## 1.2 Institutional Context

The application is branded for:

**Government Degree College Pulwama (Boys)**  
Short name: **GDC Pulwama**  
Product name: **CampusCore**

The application uses branding resources for the splash screen, login screen, navigation drawer header, toolbar, dashboard messages, logo integration, and About screen. Branding strings are defined in `strings.xml`, including `college_name`, `college_short_name`, `app_tagline`, and `toolbar_brand_compact`.

## 1.3 Purpose

The purpose of CampusCore is to reduce fragmentation in academic workflows by providing one platform where:

- Administrators define academic structure and institutional records.
- Teachers work only with assigned classes and subjects.
- Students access their attendance and study material.
- Notes and attendance are stored as structured data linked to departments, semesters, sections, subjects, and teaching assignments.
- Public academic and technology updates are aggregated through RSS feeds.

---

# Chapter 2: Problem Statement

Academic institutions often manage attendance, study material, student records, teacher assignments, and academic announcements through separate manual or semi-digital processes. This creates several issues:

- Student and teacher records may be difficult to validate consistently.
- Attendance records can become decentralized and hard to audit.
- Teachers may upload notes in unstructured channels.
- Students may not have one place to view notes and attendance.
- Administrators may lack a compact mobile interface for academic structure and assignment management.
- Role-based access may be weak when workflows are handled informally.

CampusCore addresses these problems by implementing a structured Android application backed by Firebase Authentication and Firestore security rules. It models institutional workflows using role-aware screens and backend rules that limit each actor to the operations relevant to their role.

---

# Chapter 3: Objectives

The major objectives of CampusCore are:

1. To provide a mobile academic management platform for GDC Pulwama.
2. To implement role-based access for administrators, teachers, and students.
3. To support Firebase Authentication login, signup, password reset, and email verification.
4. To maintain student records through admin-created pending student profiles.
5. To maintain teacher records through admin-created pending teacher profiles.
6. To model departments, subjects, and teaching assignments in Firestore.
7. To restrict teacher attendance and notes workflows to active teaching assignments.
8. To allow teachers to mark daily attendance for assigned classes.
9. To create attendance sessions and attendance records with a 24-hour correction window.
10. To allow students to view their attendance history and subject-wise summaries.
11. To allow teachers to upload PDF notes through Cloudinary and store note metadata in Firestore.
12. To allow students to view notes matching their department and semester.
13. To provide academic and technology updates through RSS feeds.
14. To document the application architecture, data model, workflows, testing, and future scope.

---

# Chapter 4: Literature Review

## 4.1 Mobile Academic Management Systems

Mobile academic systems are used to bring institutional workflows closer to students and teachers. Compared with desktop-only systems, mobile systems improve accessibility because students and teachers can check records, upload notes, or view updates from personal devices. CampusCore follows this mobile-first approach by implementing dashboards for each role and using Android-native components.

## 4.2 Cloud-Backed Backend-as-a-Service

Firebase is widely used in academic and student projects because it provides authentication, real-time/cloud database services, security rules, and mobile SDKs. CampusCore uses Firebase Authentication for identity and Cloud Firestore for academic data. Instead of implementing a custom server, it relies on Firestore security rules to enforce access control at the database layer.

## 4.3 Role-Based Access Control

Role-Based Access Control, or RBAC, assigns permissions based on user roles. CampusCore implements three major roles: admin, teacher, and student. The application uses these roles in Java navigation logic and Firestore security rules. Admin users have broad management access. Teachers access assigned classes and uploaded notes. Students access personal attendance and department-semester notes.

## 4.4 Repository Pattern in Android

The repository pattern separates data access from UI components. CampusCore implements repositories such as `FirebaseUserRepository`, `AttendanceRepository`, `NotesRepository`, `StudentRepository`, `AcademicStructureRepository`, `TeachingAssignmentsRepository`, and `CampusCoreUpdatesRepository`. Fragments and activities call repositories through callback interfaces rather than directly embedding Firestore or network operations everywhere.

## 4.5 Cloud File Storage and External APIs

CampusCore uses Cloudinary raw uploads for PDF notes. It uses OkHttp to upload selected PDF bytes to Cloudinary and stores the returned HTTPS `secure_url` in Firestore. RSS feed integration is also implemented with OkHttp and XML parsing for academic updates.

---

# Chapter 5: Requirement Analysis

## 5.1 Functional Requirements

The implementation supports the following functional requirements:

- Splash screen routes users based on authentication state and profile lookup.
- Login screen authenticates users using email and password.
- Signup links student accounts through roll number and registration number.
- Teacher signup links teacher accounts through employee ID.
- Email verification is required before dashboard access.
- Forgot password sends reset emails through Firebase Authentication.
- Admin dashboard uses a navigation drawer for management modules.
- Student and teacher dashboards use bottom navigation.
- Admin can manage students, pending teachers, departments, subjects, and teaching assignments.
- Teachers can view assigned classes, mark attendance, and upload notes.
- Students can view attendance records and notes.
- Users can view RSS-based academic updates.
- Profile placeholder displays role-specific user identity.
- About screen displays read-only app and institution details.

## 5.2 Non-Functional Requirements

- Authentication must be handled through Firebase Authentication.
- Data must be stored in Firestore collections.
- Role-based access must be enforced by Firestore rules.
- PDF uploads must accept only PDFs and enforce a maximum file size of 10 MB.
- Network-dependent actions must handle offline or failure states.
- UI must remain responsive through asynchronous Firebase and OkHttp calls.
- The application must compile and pass unit tests.

## 5.3 Hardware and Software Requirements

Development environment:

- Android Studio project structure
- Java 11 compatibility
- Gradle Kotlin build scripts
- Android Gradle Plugin through version catalog
- Firebase configuration through `google-services.json`

Runtime requirements:

- Android device or emulator with minimum SDK 24
- Internet connectivity for Firebase, Cloudinary, and RSS updates
- PDF viewer app for opening downloaded PDFs

---

# Chapter 6: Project Analysis

## 6.1 Project Purpose

CampusCore is an official academic management platform for GDC Pulwama. Its purpose is to digitize core academic workflows: authentication, onboarding, academic structure management, student management, teacher management, assignment-based class mapping, attendance, notes, and updates.

## 6.2 Business Problem Solved

The system solves the problem of scattered academic data and manual coordination. It centralizes:

- Who belongs to the institution.
- Which students belong to which department, semester, and section.
- Which teachers are valid and active.
- Which subjects belong to each department and semester.
- Which teacher is assigned to which subject/class.
- Which attendance entries belong to which session.
- Which notes belong to which subject and class context.

## 6.3 Application Scope

CampusCore covers:

- Authentication and account verification.
- Student and teacher onboarding through pre-created records.
- Admin management of academic records.
- Assignment-driven teaching workflows.
- Teacher attendance and notes workflows.
- Student attendance and notes access.
- RSS update browsing.
- Institutional branding and About screen.

The application does not implement payments, hostel management, library management, exam result publishing, timetable generation, chat, or biometric attendance in the observed codebase.

## 6.4 User Roles

| Role | Implementation Source | Responsibilities |
|---|---|---|
| Admin | `AppRoles.ADMIN`, admin dashboard, Firestore rules | Manage students, pending teachers, departments, subjects, teaching assignments, and broad records |
| Teacher | `AppRoles.TEACHER`, teacher dashboard, assignment rules | Mark attendance, upload notes, access assigned classes, view updates |
| Student | `AppRoles.STUDENT`, student dashboard | View attendance, notes, updates, and profile |

## 6.5 Feature Set

Implemented features observed in the project:

- Splash routing with session/profile checks.
- Login, signup, forgot password, verify email.
- Admin navigation drawer and management fragments.
- Student and teacher bottom-navigation dashboards.
- Home dashboard cards with role-specific feature cards.
- Academic structure management for departments and subjects.
- Student management with pending student and users collection synchronization.
- Teacher management through `pendingTeachers`.
- Teaching assignments with normalization and validation.
- Attendance marking through assignment-scoped sessions and records.
- Student attendance history and subject-wise summary.
- Notes upload to Cloudinary and note metadata in Firestore.
- Notes list and PDF open/download flow.
- RSS updates with category and subject filtering.
- Institutional branding and About screen.

## 6.6 Technical Stack

| Layer | Technology |
|---|---|
| Programming language | Java |
| UI | Android XML, Material Components, RecyclerView, DrawerLayout, BottomNavigationView |
| Binding | Android ViewBinding |
| Authentication | Firebase Authentication |
| Database | Cloud Firestore |
| File upload | Cloudinary raw upload API |
| Networking | OkHttp |
| Image loading | Glide |
| Feed parsing | Android XmlPullParser |
| Build system | Gradle Kotlin DSL |
| Rules | Firestore security rules, Firebase Storage rules |

## 6.7 Architecture Style

CampusCore uses a layered Android architecture:

- **Activity layer:** launches and owns navigation containers.
- **Fragment layer:** implements screens and user interactions.
- **Adapter layer:** binds model lists to RecyclerViews.
- **Repository layer:** performs Firebase, Firestore, Cloudinary, and RSS operations.
- **Model layer:** represents Firestore documents and UI data.
- **Utility layer:** centralizes constants, validation, roles, navigation, network checks, and PDF helpers.
- **Security rule layer:** enforces backend authorization.

The app is not implemented with MVVM or Jetpack Compose in the observed source. It uses Java callbacks and repositories with ViewBinding.

## 6.8 Authentication Flow

Authentication is implemented in `FirebaseUserRepository` and `AcademicOnboardingManager`.

Observed flow:

1. User opens app through `MainActivity`, which launches `SplashActivity`.
2. Splash checks whether a Firebase user exists.
3. If no user exists, app opens `LoginActivity`.
4. If a user exists, app fetches Firestore profile.
5. Login uses `signInWithEmailAndPassword`.
6. Signup uses `createUserWithEmailAndPassword`.
7. Student signup links to `pendingStudents` using roll number and registration number.
8. Teacher signup links to `pendingTeachers` using employee ID.
9. Email verification is sent after signup.
10. `VerifyEmailActivity` lets users resend or refresh verification.
11. Navigation routes users to admin, teacher, or student dashboard based on `role`.

## 6.9 Authorization Model

Authorization exists at two levels:

- UI-level navigation selects available screens by role.
- Firestore security rules enforce backend access by role and assignment relationships.

Firestore rule functions include `isAdmin()`, `isTeacher()`, `isStudent()`, `userDepartment()`, `teacherCanWriteAttendanceSession()`, `teacherCanWriteAttendance()`, and `teacherCanWriteNote()`.

## 6.10 Firestore Collections

The implemented collection constants are:

- `users`
- `attendanceRecords`
- `attendanceSessions`
- `notes`
- `departments`
- `subjects`
- `pendingStudents`
- `pendingTeachers`
- `teachingAssignments`

## 6.11 Data Relationships

CampusCore is built around academic relationships:

- A user has a role.
- A student user belongs to department, semester, section, batch, roll number, and registration number.
- A teacher user has employee ID, designation, and primary department.
- A department has subjects.
- A subject belongs to a department and semester.
- A teaching assignment links teacher, subject, department, semester, and section.
- Attendance sessions are created for teaching assignments and dates.
- Attendance records belong to sessions and students.
- Notes belong to teacher uploads and assignment/subject contexts.

## 6.12 Academic Workflow

The academic workflow begins with admin-created structure:

1. Admin creates departments.
2. Admin creates subjects mapped to departments and semesters.
3. Admin creates pending student records.
4. Admin creates pending teacher records.
5. Admin creates teaching assignments.
6. Students and teachers activate accounts by matching pending records.
7. Teachers perform attendance and notes workflows from active assignments.
8. Students view filtered academic content.

## 6.13 Attendance Workflow

Observed in `AttendanceRepository`, `MarkAttendanceFragment`, and attendance models:

1. Teacher selects an active teaching assignment.
2. Repository fetches students by department, semester, and section.
3. Attendance defaults can be displayed with present/absent toggles.
4. Repository checks if a session already exists for assignment and date.
5. A session ID is built from `assignmentId + "_" + date`.
6. Attendance session stores summary totals, lock flag, submission time, editable-until timestamp, and metadata.
7. Attendance records are written for each student.
8. Existing sessions can be edited only while `editableUntil` is in the future.
9. Student users fetch attendance records by `studentUid`.
10. Teacher users fetch sessions or records by `teacherUid`.

## 6.14 Notes Workflow

Observed in `NotesRepository`, `UploadNotesFragment`, `StudentNotesFragment`, `PdfOpenUtils`, and Cloudinary constants:

1. Teacher selects a PDF.
2. UI validates PDF type and maximum size.
3. PDF bytes are uploaded to Cloudinary using the unsigned upload preset `campuscore_notes`.
4. Cloudinary returns an HTTPS `secure_url`.
5. Firestore stores note metadata including note ID, title, subject code/name, assignment ID, department, semester, uploader UID/name, PDF URL, file name, and timestamp.
6. Teacher notes are fetched by `uploadedByUid`.
7. Student notes are fetched by department and semester.
8. Remote PDFs are downloaded to local cache and opened with a PDF viewer through `FileProvider`.

## 6.15 Updates Workflow

Observed in `CampusCoreUpdatesRepository`, `UpdatesFeedProvider`, and `CampusCoreUpdatesFragment`:

1. Repository selects RSS sources based on department.
2. OkHttp fetches XML feeds.
3. XML parser extracts items from RSS `item` or Atom `entry`.
4. HTML content is cleaned.
5. Items are deduplicated by article URL or fallback stable ID.
6. Items are sorted newest first.
7. Maximum feed items are capped at `MAX_FEED_ITEMS = 80`.
8. UI filters updates by category and subject terms.

## 6.16 Student Management Workflow

Observed in `StudentRepository` and admin fragments:

1. Admin creates or edits a student record.
2. Record is saved to `pendingStudents` using ID `rollNumber_registrationNumber`.
3. If a linked UID exists, record is also saved to `users`.
4. Unique checks are performed for roll number and registration number.
5. Student signup uses roll number and registration number to claim the pending record.
6. After claim, the `users/{uid}` document is created and pending record is updated with UID/email.

## 6.17 Teacher Management Workflow

Observed in `TeachingAssignmentsRepository`:

1. Admin creates a pending teacher record by employee ID.
2. Pending teacher includes name, primary department, designation, email, UID, and active flag.
3. Teacher signup claims the pending teacher record using employee ID.
4. Teacher profile is created in `users`.
5. Existing teaching assignments can be linked to the teacher UID.
6. Deleting/deactivating a teacher deactivates related teaching assignments and removes the pending teacher record.

## 6.18 Department and Subject Management Workflow

Observed in `AcademicStructureRepository`:

1. Departments are stored with `departmentId`, `departmentName`, `isActive`, and timestamp.
2. Subjects are stored with `subjectCode`, `subjectName`, `departmentId`, `semester`, `isActive`, and timestamp.
3. Active department and subject queries support dashboard and forms.
4. Fallback defaults exist in `AcademicDataProvider` if Firestore structure is unavailable.
5. Deleting a subject deactivates linked teaching assignments before deleting the subject.

## 6.19 Teaching Assignment Workflow

Observed in `TeachingAssignmentsRepository` and `TeachingAssignmentNormalizer`:

1. Admin selects teacher, subject, department, semester, and section.
2. Repository validates teacher record.
3. Repository validates subject record.
4. Repository validates department record.
5. Normalizer fills teacher name, employee ID, subject name, department label, and normalized fields.
6. Assignment ID is generated using teacher identity, subject code, department, semester, and section when absent.
7. Assignment is stored in `teachingAssignments`.
8. Teachers fetch active assignments by `teacherUid`.
9. Teacher account linking can update assignments previously created against employee ID.

## 6.20 Branding Structure

Branding resources include:

- `app_name`: CampusCore
- `college_name`: Government Degree College Pulwama (Boys)
- `college_short_name`: GDC Pulwama
- `app_tagline`: Official Academic Portal
- `toolbar_brand_compact`: CampusCore - GDC Pulwama in compact branding text
- Reusable logo resource: `ic_campuscore_logo.xml`
- About screen: `AboutCampusCoreFragment`

---

# Chapter 7: System Architecture

## 7.1 Architecture Overview

CampusCore follows a client-cloud architecture. The Android app is the client. Firebase Authentication provides identity. Cloud Firestore stores application data. Firestore security rules control access. Cloudinary stores uploaded PDF files. RSS feeds provide external academic updates.

The application has no custom backend server in the observed implementation. All application logic is either client-side Java or Firestore rule enforcement. This makes rule quality critical because Firestore rules act as the security boundary.

## 7.2 Application Layers

### Activity Layer

Activities provide entry points and navigation containers:

- `MainActivity` launches `SplashActivity`.
- `SplashActivity` checks session state.
- `LoginActivity`, `SignupActivity`, `ForgotPasswordActivity`, and `VerifyEmailActivity` handle authentication screens.
- `AdminDashboardActivity` hosts admin drawer navigation.
- `TeacherDashboardActivity` hosts teacher bottom navigation.
- `StudentDashboardActivity` hosts student bottom navigation.

### Fragment Layer

Fragments implement screens:

- `HomeFragment`
- `AttendanceFragment`
- `MarkAttendanceFragment`
- `StudentAttendanceFragment`
- `NotesFragment`
- `UploadNotesFragment`
- `StudentNotesFragment`
- `CampusCoreUpdatesFragment`
- `ProfileFragment`
- Admin management fragments for students, departments, subjects, pending teachers, and teaching assignments.
- Add/edit fragments for departments, subjects, students, and teaching assignments.
- `AboutCampusCoreFragment`

### Repository Layer

Repositories isolate data operations:

| Repository | Responsibility |
|---|---|
| `FirebaseUserRepository` | Login, signup delegation, current user profile, verification, password reset |
| `StudentRepository` | Student records, pending student records, account linking |
| `TeachingAssignmentsRepository` | Teacher records, pending teachers, assignments, assignment linking |
| `AcademicStructureRepository` | Departments, subjects, fallback academic structure |
| `AttendanceRepository` | Attendance sessions, records, history |
| `NotesRepository` | Cloudinary upload, notes metadata, note queries, delete |
| `CampusCoreUpdatesRepository` | RSS feed retrieval, parsing, filtering |

### Model Layer

Models represent Firestore or UI data:

- `UserModel`
- `DepartmentModel`
- `SubjectModel`
- `PendingTeacherModel`
- `TeachingAssignmentModel`
- `AttendanceSessionModel`
- `AttendanceModel`
- `StudentAttendanceItem`
- `SubjectAttendanceSummary`
- `NotesModel`
- `FeedItemModel`
- `FeatureCard`

### Utility Layer

Utilities centralize cross-cutting constants and helpers:

- `AppRoles`
- `FirestoreCollections`
- `FirestoreFields`
- `AttendanceConstants`
- `CloudinaryConstants`
- `UpdatesConstants`
- `AcademicDataProvider`
- `ValidationUtils`
- `NetworkUtils`
- `NavigationUtils`
- `PdfOpenUtils`
- `SnackbarUtils`

## 7.3 Repository Pattern Usage

The app uses repository classes to keep Firestore and network code out of most UI fragments. This reduces duplication and gives a single place for readable error messages, parsing, sorting, and fallback behavior. Repositories communicate with UI using `FirestoreCallback<T>`, which defines `onSuccess(T data)` and `onError(String message)`.

## 7.4 Firebase Authentication Architecture

Firebase Authentication provides:

- Email/password login.
- Email/password signup.
- Password reset emails.
- Email verification emails.
- Current user identity.
- Logout.

The app links Firebase Auth users with Firestore profile documents in `users`. Auth UID is the canonical document ID for linked users.

## 7.5 Firestore Architecture

Firestore stores normalized academic data. The architecture uses collection-level separation:

- Identity and role data in `users`.
- Pre-activation student data in `pendingStudents`.
- Pre-activation teacher data in `pendingTeachers`.
- Academic structure in `departments` and `subjects`.
- Teacher-class mapping in `teachingAssignments`.
- Attendance summary sessions in `attendanceSessions`.
- Per-student attendance rows in `attendanceRecords`.
- Notes metadata in `notes`.

## 7.6 Cloudinary Integration

Cloudinary is used as external file storage for PDF notes. The app uses:

- Cloud name: `dpot51lpj`
- Unsigned upload preset: `campuscore_notes`
- Raw upload endpoint: `https://api.cloudinary.com/v1_1/dpot51lpj/raw/upload`
- MIME type: `application/pdf`
- Max PDF size: 10 MB

The application stores only the secure PDF URL in Firestore.

## 7.7 RSS Feed Architecture

RSS sources are defined in `UpdatesFeedProvider`. Sources include MIT Technology Review, IEEE Spectrum, freeCodeCamp, arXiv Computer Science, and TechCrunch for Computer Science or Information Technology contexts. `CampusCoreUpdatesRepository` retrieves feed XML, parses items, cleans HTML descriptions, deduplicates articles, and sorts latest items first.

## 7.8 Assignment-Scoped Design

Assignment-scoped design is one of the strongest architectural points in CampusCore. Teacher workflows for attendance and notes are not based only on user role. They also require a valid active teaching assignment connecting:

- teacher UID or employee ID
- subject code
- department ID
- semester
- section

This prevents a teacher from writing attendance or notes for arbitrary classes.

## 7.9 Role-Based Access Control

RBAC is implemented in:

- `AppRoles`
- Dashboard routing in `NavigationUtils`
- Activity navigation logic
- Firestore rules

Admin users can manage academic structure. Teachers access assignment-based teaching operations. Students access their own attendance and department-semester notes.

---

# Chapter 8: Database Design

## 8.1 Collection Summary

| Collection | Purpose |
|---|---|
| `users` | Auth-linked user profiles for admins, teachers, and students |
| `pendingStudents` | Admin-created student records before account linking |
| `pendingTeachers` | Admin-created teacher records before account linking |
| `departments` | Institutional departments |
| `subjects` | Subjects mapped to departments and semesters |
| `teachingAssignments` | Teacher-to-class/subject assignment records |
| `attendanceSessions` | Attendance summary per assignment/date |
| `attendanceRecords` | Per-student attendance records |
| `notes` | PDF notes metadata and Cloudinary URL |

## 8.2 `users`

Purpose: Stores linked application profiles.

Fields observed through `UserModel` and repository usage:

- `uid`
- `name`
- `email`
- `role`
- `department`
- `departmentId`
- `primaryDepartmentId`
- `employeeId`
- `designation`
- `semester`
- `rollNumber`
- `section`
- `batch`
- `registrationNumber`
- `assignedSubjects`

Relationships:

- Student `uid` links to attendance records by `studentUid`.
- Teacher `uid` links to teaching assignments by `teacherUid`.
- User role determines dashboard and Firestore permissions.

## 8.3 `pendingStudents`

Purpose: Stores admin-created student records before Firebase account activation.

Document ID:

- `rollNumber_registrationNumber`

Important fields:

- `uid`
- `name`
- `email`
- `role`
- `department`
- `departmentId`
- `semester`
- `rollNumber`
- `section`
- `batch`
- `registrationNumber`

Workflow:

- Admin creates pending record.
- Student signs up with roll number and registration number.
- Repository creates `users/{uid}` and updates pending record with UID/email.

## 8.4 `pendingTeachers`

Purpose: Stores admin-created teacher records before account activation.

Document ID:

- normalized `employeeId`

Fields:

- `employeeId`
- `name`
- `primaryDepartmentId`
- `designation`
- `email`
- `uid`
- `isActive`
- `timestamp`

Workflow:

- Admin saves teacher record.
- Teacher signs up with employee ID.
- Repository creates `users/{uid}` and updates pending teacher with UID/email.
- Assignments can be linked to teacher UID.

## 8.5 `departments`

Purpose: Stores academic departments.

Document ID:

- normalized department ID such as `CS`, `IT`, `ECE`, `ME`, `CE`

Fields:

- `departmentId`
- `departmentName`
- `isActive`
- `timestamp`

Constraints:

- Department ID is required.
- Admin-only create/update/delete in rules.
- All signed-in users can read departments.

## 8.6 `subjects`

Purpose: Stores subjects mapped to department and semester.

Document ID:

- normalized subject code

Fields:

- `subjectCode`
- `subjectName`
- `departmentId`
- `semester`
- `isActive`
- `timestamp`

Relationships:

- Teaching assignments reference `subjectCode`.
- Notes and attendance reference subject code/name.
- Deleting a subject deactivates linked assignments.

## 8.7 `teachingAssignments`

Purpose: Stores teacher-class-subject mappings.

Fields:

- `assignmentId`
- `teacherUid`
- `employeeId`
- `teacherName`
- `subjectCode`
- `subjectName`
- `departmentId`
- `departmentLabel`
- `semester`
- `section`
- `isActive`
- `assignedBy`
- `timestamp`

Document ID generation:

- Uses teacher owner, subject code, department ID, semester, and section.

Relationships:

- Attendance sessions reference `assignmentId`.
- Notes reference `assignmentId`.
- Teachers fetch assignments by `teacherUid`.
- Firestore rules validate writes against active assignments.

## 8.8 `attendanceSessions`

Purpose: Stores one attendance session summary per assignment/date.

Fields:

- `sessionId`
- `assignmentId`
- `teacherUid`
- `teacherName`
- `employeeId`
- `subjectCode`
- `subjectName`
- `departmentId`
- `semester`
- `section`
- `date`
- `totalStudents`
- `presentCount`
- `absentCount`
- `isLocked`
- `submittedAt`
- `editableUntil`
- `lastModifiedAt`
- `lastModifiedBy`
- `timestamp`

Important rule:

- Create requires `editableUntil > request.time`.
- Update requires `request.time < resource.data.editableUntil`.

## 8.9 `attendanceRecords`

Purpose: Stores per-student attendance entries.

Fields:

- `attendanceId`
- `sessionId`
- `assignmentId`
- `studentUid`
- `studentName`
- `rollNumber`
- `registrationNumber`
- `teacherUid`
- `subject`
- `semester`
- `department`
- `date`
- `status`
- `timestamp`

Allowed status:

- `Present`
- `Absent`

Relationships:

- Records are linked to `attendanceSessions` by `sessionId`.
- Records are linked to students by `studentUid`.
- Records are linked to teachers by `teacherUid`.

## 8.10 `notes`

Purpose: Stores metadata for teacher-uploaded PDF notes.

Fields:

- `noteId`
- `title`
- `subjectCode`
- `subjectName`
- `assignmentId`
- `department`
- `semester`
- `uploadedByUid`
- `uploadedByName`
- `pdfUrl`
- `fileName`
- `timestamp`

Constraints:

- PDF URL must start with HTTPS according to Firestore rules.
- Teacher must be assigned to the relevant subject/department/semester.
- Students can read notes for their department and semester.

---

# Chapter 9: Implementation

## 9.1 Android Manifest

The manifest declares:

- Internet permission.
- Network state permission.
- PDF MIME query for opening PDFs.
- FileProvider for cached PDFs.
- Activities for admin, teacher, student dashboards.
- Auth activities including login, signup, forgot password, verify email, and splash.

## 9.2 Gradle Configuration

The app uses:

- Android namespace `com.example.campuscore`
- Application ID `com.example.campuscore`
- Minimum SDK 24
- Target SDK 36
- Java 11 source/target compatibility
- ViewBinding enabled
- Firebase Auth, Firestore, Analytics
- Material, AppCompat, RecyclerView, CardView, DrawerLayout, SwipeRefreshLayout
- OkHttp and Glide

## 9.3 Splash Implementation

`SplashActivity` uses `FirebaseUserRepository` to check the current user and route the user to the appropriate dashboard. Branding is displayed through the splash layout with CampusCore, official portal tagline, and GDC Pulwama institution name.

## 9.4 Login Implementation

`LoginActivity` validates email and password using `ValidationUtils`, calls `FirebaseUserRepository.login`, checks email verification, and opens the correct dashboard through `NavigationUtils`. It also links to signup and forgot password screens.

## 9.5 Signup Implementation

`SignupActivity` supports account activation through institutional identifiers. Student signup uses roll number and registration number. Teacher signup uses employee ID through repository/service flow. Account creation is tied to pending records rather than open self-registration.

## 9.6 Dashboard Implementation

Admin dashboard:

- Uses `DrawerLayout` and `NavigationView`.
- Navigates to admin modules and About screen.
- Uses toolbar synchronization with current fragment.

Teacher and student dashboards:

- Use `BottomNavigationView`.
- Navigate to home, attendance, notes, updates, and profile.

## 9.7 Attendance Implementation

Attendance is implemented with assignment-scoped sessions:

- Teacher fetches active assignments.
- Assignment determines department, semester, section, and subject.
- Students are loaded for the assignment class.
- Session is created per assignment/date.
- Records are written in a Firestore batch.
- Correction window is 24 hours.

## 9.8 Notes Implementation

PDF selection and validation occur in the UI. Upload is performed by `NotesRepository` using a custom `ProgressRequestBody`. Firestore metadata is written after Cloudinary returns a valid secure URL. PDF opening uses `PdfOpenUtils`, OkHttp download, cache storage, FileProvider URI generation, and ACTION_VIEW intent.

## 9.9 Updates Implementation

Updates are fetched from public RSS sources. `CampusCoreUpdatesRepository` runs network requests on a single-thread executor and posts results back to the main thread. It parses XML using `XmlPullParser`.

## 9.10 About Screen Implementation

The read-only `AboutCampusCoreFragment` displays:

- App Name: CampusCore
- Institution: Government Degree College Pulwama (Boys)
- Version: 1.0 MVP
- Purpose: Academic Management Platform
- Technologies: Java, Firebase, Firestore, Cloudinary

---

# Chapter 10: Module Documentation

## 10.1 Authentication Module

Classes:

- `FirebaseUserRepository`
- `AcademicOnboardingManager`
- `LoginActivity`
- `SignupActivity`
- `ForgotPasswordActivity`
- `VerifyEmailActivity`
- `SplashActivity`

Responsibilities:

- User login.
- Password reset.
- Student and teacher account creation.
- Email verification.
- Session checking.
- Dashboard routing.

## 10.2 Admin Module

Classes and fragments:

- `AdminDashboardActivity`
- `ManageStudentsFragment`
- `ManagePendingTeachersFragment`
- `ManageDepartmentsFragment`
- `ManageSubjectsFragment`
- `ManageTeachingAssignmentsFragment`
- Add/edit fragments for students, departments, subjects, and assignments.

Responsibilities:

- Maintain institutional data.
- Manage pending records.
- Define assignments.
- Navigate role-specific admin functions.

## 10.3 Student Module

Classes:

- `StudentDashboardActivity`
- `StudentAttendanceFragment`
- `StudentNotesFragment`
- `HomeFragment`
- `ProfileFragment`

Responsibilities:

- View attendance.
- View notes.
- View updates.
- View profile placeholder.

## 10.4 Teacher Module

Classes:

- `TeacherDashboardActivity`
- `MarkAttendanceFragment`
- `UploadNotesFragment`
- `HomeFragment`
- `ProfileFragment`

Responsibilities:

- Access assigned classes.
- Mark attendance.
- Upload notes.
- View updates.

## 10.5 Updates Module

Classes:

- `CampusCoreUpdatesFragment`
- `CampusCoreUpdatesRepository`
- `UpdatesFeedProvider`
- `FeedSource`
- `FeedItemModel`
- `CampusCoreUpdatesAdapter`

Responsibilities:

- Fetch public RSS feeds.
- Parse update items.
- Filter by category and subject.
- Open external article URLs.

---

# Chapter 11: Security Architecture

## 11.1 Firestore Security Rules

Firestore rules define functions for role detection and workflow validation. Main role functions:

- `signedIn()`
- `currentUser()`
- `isAdmin()`
- `isTeacher()`
- `isStudent()`

## 11.2 User Security

Users can read their own profile. Admins can manage users. Teachers can read student users in their department. New user creation is allowed only for admin or for signed-in users whose Firestore data matches pending student/teacher records.

## 11.3 Attendance Security

Attendance creation and update require:

- Teacher role.
- Valid attendance data fields.
- Existing active teaching assignment.
- Session consistency.
- Edit request within the editable window.

Students can read only records where `studentUid` equals their UID. Teachers can read records where `teacherUid` equals their UID. Admins can read all attendance records.

## 11.4 Notes Security

Teacher note writes require:

- Teacher role.
- Valid note fields.
- HTTPS PDF URL.
- Matching active teaching assignment.

Students can read notes only when department matches their department and semester matches their semester.

## 11.5 Academic Structure Security

Departments and subjects are readable by signed-in users. Admins can create, update, or delete them.

## 11.6 Pending Record Security

Pending student and pending teacher records support controlled onboarding. The rules allow account claim updates only when request data matches the pending record and authenticated email/UID.

## 11.7 Storage Rules

Firebase Storage rules allow signed-in users to read notes paths and teachers to write note files under `notes/{department}/{semester}/{subjectCode}/{fileName}` with PDF content type and size below 10 MB. The current implementation uses Cloudinary for note upload, while storage rules remain present.

---

# Chapter 12: Testing and Validation

## 12.1 Build Verification

The project was verified with:

- `./gradlew.bat assembleDebug`
- `./gradlew.bat testDebugUnitTest`

Both commands completed successfully after the branding changes in the current workspace.

## 12.2 Unit Testing Report

The source tree includes `ExampleUnitTest.java`. The current unit test task runs successfully. The implemented application relies more heavily on integration behavior through Firebase, Firestore, Cloudinary, and Android UI workflows than on pure unit-testable business functions.

Recommended unit test targets:

- `ValidationUtils`
- `TeachingAssignmentNormalizer`
- `AcademicDataProvider`
- Date/session ID building in `AttendanceRepository`
- Feed filtering in `CampusCoreUpdatesRepository`

## 12.3 Integration Testing Report

Integration points:

- Firebase Auth login/signup/reset/verification.
- Firestore user/profile lookup.
- Pending student linking.
- Pending teacher linking.
- Department and subject CRUD.
- Assignment save and assignment repair.
- Attendance session and record batch writes.
- Cloudinary upload and Firestore note metadata write.
- RSS feed retrieval and parsing.
- PDF download and FileProvider open.

## 12.4 Functional Testing Matrix

| Module | Test Scenario | Expected Result |
|---|---|---|
| Splash | Existing signed-in user opens app | Profile fetched and role dashboard opened |
| Login | Valid credentials | User authenticated and routed by role |
| Login | Invalid credentials | Readable error displayed |
| Signup | Student uses matching roll and registration | Firebase user created and linked to pending student |
| Signup | Teacher uses matching employee ID | Firebase user created and linked to pending teacher |
| Verification | Unverified user logs in | Verify email screen opened |
| Admin departments | Save department | Firestore department document created/updated |
| Admin subjects | Save subject | Firestore subject document created/updated |
| Admin students | Save student | Pending student record created and uniqueness checked |
| Admin teachers | Save teacher | Pending teacher record created/updated |
| Teaching assignments | Save valid assignment | Assignment normalized and stored |
| Teacher attendance | Submit attendance | Session and records written in batch |
| Teacher attendance | Edit after correction window | Error prevents modification |
| Student attendance | Open attendance screen | Records filtered by current student UID |
| Notes upload | Upload valid PDF | Cloudinary URL saved in Firestore |
| Notes upload | Upload non-PDF or oversized PDF | UI rejects file |
| Student notes | Open notes | Notes filtered by department and semester |
| Updates | Fetch RSS | Feed items displayed if sources respond |
| About | Open About screen | Read-only app information displayed |

## 12.5 Module Validation Summary

| Module | Validation Strategy |
|---|---|
| Authentication | Firebase exceptions mapped to readable messages |
| Signup | Email, password, roll/registration, and employee identifiers validated |
| Attendance | Assignment validation, session ID, editableUntil, status values |
| Notes | MIME type, PDF signature flow, size, HTTPS Cloudinary URL |
| Academic structure | Required codes and active flags |
| Updates | Network fetch failure handling and empty state |
| PDF open | URL validation, download response validation, PDF signature scan |

## 12.6 Security Validation Summary

| Security Area | Implementation |
|---|---|
| Role detection | Firestore functions inspect `users/{uid}.role` |
| Student access | Own attendance and department-semester notes |
| Teacher access | Own assignments, own attendance sessions, own notes |
| Admin access | Management across collections |
| Assignment enforcement | Attendance and notes writes require matching active assignment |
| Onboarding control | Pending records must match identity claims |
| PDF URL validation | Notes require HTTPS URL |
| Correction window | Attendance updates require `request.time < editableUntil` |

---

# Chapter 13: Results and Discussion

CampusCore successfully implements a working academic management platform for the documented scope. The most important result is the assignment-driven workflow model. Instead of giving teachers broad access to all students or subjects, the system maps teachers to concrete assignments and uses those assignments for attendance and notes.

The application also demonstrates practical Firebase usage. Authentication handles identity, Firestore stores structured academic data, rules enforce access control, and repositories keep data operations separated from UI code. Cloudinary integration gives the notes module file upload capability without building a separate file server.

The UI structure is practical for the roles. Admin uses a navigation drawer because the admin role has many management modules. Student and teacher roles use bottom navigation because their workflows are fewer and frequently accessed.

The current implementation is suitable as an MVP for a final-year BCA Honours mini project. It includes real data modeling, role-based security, cloud integration, and multiple connected workflows.

---

# Chapter 14: Conclusion

CampusCore is an Android academic management platform implemented for Government Degree College Pulwama (Boys). It provides structured digital workflows for admin, teacher, and student users. The application uses Java, Android XML, ViewBinding, Firebase Authentication, Cloud Firestore, Firestore security rules, Cloudinary, OkHttp, and Glide.

The project demonstrates key software engineering concepts: layered design, repository pattern, role-based access control, data modeling, asynchronous callbacks, validation, error handling, and integration with cloud services. Its strongest design principle is assignment-scoped academic control, where teacher actions are constrained by active teaching assignments.

The system meets the major objectives of a BCA Honours mini project by solving a real institutional problem with a functional Android application and a cloud-backed database architecture.

---

# Chapter 15: Future Scope

Future improvements can include:

1. Push notifications for attendance, notes, and updates.
2. Timetable module linked with teaching assignments.
3. Internal announcements module with admin publishing.
4. Offline caching for attendance and notes metadata.
5. Dashboard analytics for attendance percentages and trends.
6. Export attendance reports to PDF or Excel.
7. Stronger unit and instrumentation test coverage.
8. Biometric or QR-assisted attendance.
9. In-app PDF viewer.
10. Supervisor approval workflows for teacher records and assignments.
11. Native admin analytics for departments, sections, and semesters.
12. Migration from callbacks to ViewModel/LiveData or Kotlin coroutines in a future architecture revision.

---

# Chapter 16: Diagram Specifications

These specifications are based on actual implemented modules.

## 16.1 Use Case Diagram Specification

Actors:

- Admin
- Teacher
- Student
- Firebase Authentication
- Cloud Firestore
- Cloudinary
- RSS Sources

Use cases:

- Login
- Signup and account activation
- Verify email
- Reset password
- Manage students
- Manage pending teachers
- Manage departments
- Manage subjects
- Manage teaching assignments
- Mark attendance
- View attendance
- Upload notes
- View notes
- Open PDF
- View updates
- View profile
- View About CampusCore

Relationships:

- Admin includes all management use cases.
- Teacher includes mark attendance, upload notes, updates, profile.
- Student includes view attendance, view notes, updates, profile.
- Signup includes pending record validation.
- Upload notes includes Cloudinary upload and Firestore metadata write.

## 16.2 DFD Level 0 Specification

Single process:

- CampusCore Academic Management System

External entities:

- Admin
- Teacher
- Student
- Firebase Authentication
- Cloud Firestore
- Cloudinary
- RSS Sources

Major data flows:

- Credentials to Firebase Authentication.
- User profile and academic records to/from Firestore.
- PDF bytes to Cloudinary.
- PDF URL metadata to Firestore.
- RSS XML from sources to Updates module.
- Attendance and notes data from Firestore to users.

## 16.3 DFD Level 1 Specification

Processes:

1. Authentication and Onboarding
2. Academic Structure Management
3. Teaching Assignment Management
4. Attendance Management
5. Notes Management
6. Updates Feed Management
7. Profile and About Display

Data stores:

- `users`
- `pendingStudents`
- `pendingTeachers`
- `departments`
- `subjects`
- `teachingAssignments`
- `attendanceSessions`
- `attendanceRecords`
- `notes`

## 16.4 System Architecture Diagram Specification

Components:

- Android UI Layer: activities, fragments, XML layouts.
- Adapter Layer: RecyclerView adapters.
- Repository Layer: Firebase, Firestore, Cloudinary, RSS repositories.
- Model Layer: Java POJOs.
- Utility Layer: constants, validation, navigation, network, PDF.
- Firebase Auth.
- Cloud Firestore.
- Firestore Security Rules.
- Cloudinary raw upload API.
- RSS providers.

Connections:

- UI calls repositories.
- Repositories read/write models.
- Repositories call Firebase/Cloudinary/RSS.
- Firestore rules authorize database operations.

## 16.5 Firestore Relationship Diagram Specification

Relationships:

- `users.uid` to `teachingAssignments.teacherUid`
- `users.uid` to `attendanceRecords.studentUid`
- `users.uid` to `attendanceRecords.teacherUid`
- `users.uid` to `notes.uploadedByUid`
- `departments.departmentId` to `subjects.departmentId`
- `subjects.subjectCode` to `teachingAssignments.subjectCode`
- `teachingAssignments.assignmentId` to `attendanceSessions.assignmentId`
- `attendanceSessions.sessionId` to `attendanceRecords.sessionId`
- `teachingAssignments.assignmentId` to `notes.assignmentId`
- `pendingStudents.rollNumber + registrationNumber` to student signup identifiers
- `pendingTeachers.employeeId` to teacher signup employee ID

## 16.6 Module Interaction Diagram Specification

Interaction example for attendance:

1. Teacher dashboard opens `MarkAttendanceFragment`.
2. Fragment calls `TeachingAssignmentsRepository.fetchTeacherAssignments`.
3. Teacher selects assignment.
4. Fragment calls `TeachingAssignmentsRepository.fetchStudentsForAssignment`.
5. Fragment calls `AttendanceRepository.saveAttendanceSession`.
6. Repository writes `attendanceSessions` and `attendanceRecords`.
7. Firestore rules validate role, assignment, and edit window.
8. Student attendance screen reads `attendanceRecords` by current UID.

---

# Chapter 17: Viva Preparation

## 17.1 Probable Viva Questions and Answers

1. **What is CampusCore?**  
CampusCore is an Android academic management platform for Government Degree College Pulwama (Boys), supporting admin, teacher, and student workflows.

2. **Which language is used?**  
The application is implemented in Java.

3. **Which backend is used?**  
Firebase Authentication and Cloud Firestore are used as backend services.

4. **What is the role of Firebase Authentication?**  
It manages login, signup, email verification, password reset, current user identity, and logout.

5. **What is the role of Firestore?**  
Firestore stores user profiles, pending records, departments, subjects, assignments, attendance, and notes metadata.

6. **What are the user roles?**  
Admin, teacher, and student.

7. **Where are roles defined?**  
Roles are defined in `AppRoles` and mirrored in Firestore user documents.

8. **How does the app choose the dashboard?**  
`NavigationUtils.openDashboard` checks the user role and opens admin, teacher, or student dashboard activity.

9. **What is the repository pattern?**  
It is a design pattern where data access is isolated in repository classes instead of being spread throughout UI code.

10. **Which repositories exist in CampusCore?**  
Firebase user, student, teaching assignments, academic structure, attendance, notes, and updates repositories.

11. **What is assignment-driven architecture?**  
It means teacher actions are controlled by active teaching assignment records linking teacher, subject, department, semester, and section.

12. **Why are teaching assignments important?**  
They restrict attendance and notes workflows to valid teacher-class-subject mappings.

13. **What collections are used in Firestore?**  
`users`, `pendingStudents`, `pendingTeachers`, `departments`, `subjects`, `teachingAssignments`, `attendanceSessions`, `attendanceRecords`, and `notes`.

14. **What is the purpose of `pendingStudents`?**  
It stores admin-created student records before students activate accounts.

15. **What is the purpose of `pendingTeachers`?**  
It stores admin-created teacher records before teacher account activation.

16. **How does student signup work?**  
The student creates a Firebase account and must match a pending student record using roll number and registration number.

17. **How does teacher signup work?**  
The teacher creates a Firebase account and must match a pending teacher record using employee ID.

18. **Why is email verification used?**  
It ensures the user has access to the registered email before dashboard access.

19. **How is attendance stored?**  
Attendance uses `attendanceSessions` for session summaries and `attendanceRecords` for per-student records.

20. **What is the attendance correction window?**  
The implementation uses a 24-hour editable window through `editableUntil`.

21. **How is a session ID generated?**  
It is built from assignment ID and date.

22. **What statuses are allowed for attendance?**  
Present and Absent.

23. **How does a student view attendance?**  
The app fetches attendance records where `studentUid` equals the current user's UID.

24. **How does a teacher view attendance history?**  
Teacher records and sessions are fetched by `teacherUid`.

25. **How are notes uploaded?**  
PDF bytes are uploaded to Cloudinary using OkHttp multipart upload.

26. **Where are uploaded PDFs stored?**  
The actual file is stored in Cloudinary, while metadata is stored in Firestore.

27. **What is stored in the `notes` collection?**  
Note ID, title, subject, assignment, department, semester, uploader, PDF URL, file name, and timestamp.

28. **How is PDF type validated?**  
The UI checks MIME type and file size; PDF opening also checks downloaded PDF signature.

29. **What is the maximum PDF size?**  
10 MB.

30. **What is OkHttp used for?**  
OkHttp is used for Cloudinary upload, remote PDF download, and RSS feed fetching.

31. **What is Glide used for?**  
Glide is used for image loading in update cards.

32. **What is ViewBinding?**  
ViewBinding generates binding classes for XML layouts and avoids manual `findViewById` usage.

33. **What is `FirestoreCallback`?**  
It is a callback interface with success and error methods used by repositories.

34. **How does the app handle Firestore permission errors?**  
Repositories map Firebase exceptions to readable user messages.

35. **What are Firestore rules?**  
They are backend security rules that decide which authenticated users can read or write documents.

36. **How do rules identify admins?**  
They read the authenticated user's Firestore profile and check the `role` field.

37. **Can any teacher mark any attendance?**  
No. Rules require a matching active teaching assignment.

38. **Can a student read another student's attendance?**  
No. Rules allow students to read only records where `studentUid` matches their UID.

39. **Can students read all notes?**  
No. They can read notes matching their department and semester.

40. **What is the purpose of `AcademicDataProvider`?**  
It provides fallback departments, semesters, sections, and subject lists.

41. **Why are fallbacks used?**  
They keep certain screens usable if Firestore academic structure is unavailable.

42. **What is the role of `TeachingAssignmentNormalizer`?**  
It normalizes and repairs assignment metadata such as teacher name, employee ID, subject name, and department label.

43. **What is the role of `PdfOpenUtils`?**  
It downloads remote PDFs to cache and opens them with an external PDF viewer using FileProvider.

44. **What is the role of `NetworkUtils`?**  
It checks whether the device has an active network connection.

45. **What is the updates module?**  
It fetches public RSS feed articles and displays academic or technology updates.

46. **Which RSS sources are used?**  
MIT Technology Review, IEEE Spectrum, freeCodeCamp, arXiv Computer Science, and TechCrunch for relevant departments.

47. **How are updates filtered?**  
By category and subject text matching.

48. **What is the admin dashboard UI pattern?**  
It uses a navigation drawer.

49. **What is the teacher/student dashboard UI pattern?**  
They use bottom navigation.

50. **What is Material UI used for?**  
Material components provide toolbars, buttons, navigation, cards, text fields, and consistent UI styling.

51. **What is the purpose of `strings.xml`?**  
It centralizes display strings and branding resources.

52. **What is the About screen?**  
It is a read-only screen that displays app name, institution, version, purpose, and technologies.

53. **What is the advantage of Cloud Firestore?**  
It provides cloud-hosted NoSQL storage with SDK integration and security rules.

54. **What is a Firestore batch write?**  
It writes multiple documents atomically as one operation.

55. **Where are batch writes used?**  
Attendance sessions/records, student linking, and teacher assignment linking use batch operations.

56. **What is the main limitation of the current MVP?**  
It has limited automated tests and no custom backend server or offline-first synchronization layer.

57. **Does the app use Jetpack Compose?**  
No. It uses XML layouts and ViewBinding.

58. **Does the app use MVVM?**  
No formal MVVM implementation is observed; repositories and callbacks are used with activities/fragments.

59. **How is role-based navigation implemented?**  
By checking `role` in the user profile and opening the correct dashboard.

60. **Why is this project suitable for BCA Honours?**  
It demonstrates mobile development, cloud integration, database design, authentication, security rules, UI design, and software engineering documentation.

---

# Chapter 18: Presentation Outline

## Slide 1: Title

- CampusCore
- Academic Management Platform
- Government Degree College Pulwama (Boys)
- BCA Honours Mini Project

## Slide 2: Problem Background

- Manual academic workflows
- Fragmented attendance and notes
- Need for role-based digital access

## Slide 3: Proposed Solution

- Android application
- Admin, teacher, and student dashboards
- Firebase-backed data and authentication

## Slide 4: Objectives

- Centralize academic operations
- Secure role-based access
- Assignment-driven attendance and notes
- Student access to records and material

## Slide 5: User Roles

- Admin
- Teacher
- Student
- Role-specific permissions

## Slide 6: Technical Stack

- Java
- Android XML and ViewBinding
- Firebase Authentication
- Cloud Firestore
- Cloudinary
- OkHttp and Glide

## Slide 7: System Architecture

- Android UI layer
- Repositories
- Models
- Firebase Auth
- Firestore
- Cloudinary
- RSS feeds

## Slide 8: Database Design

- `users`
- `pendingStudents`
- `pendingTeachers`
- `departments`
- `subjects`
- `teachingAssignments`
- `attendanceSessions`
- `attendanceRecords`
- `notes`

## Slide 9: Admin Features

- Manage students
- Manage teachers
- Manage departments and subjects
- Manage teaching assignments

## Slide 10: Teacher Features

- View assigned classes
- Mark attendance
- Upload PDF notes
- View updates

## Slide 11: Student Features

- View attendance
- View notes
- Open PDFs
- View updates

## Slide 12: Security Model

- Firebase Auth identity
- Firestore role functions
- Assignment-scoped writes
- Department and semester filters

## Slide 13: Testing and Validation

- Build verification
- Unit test task
- Functional testing matrix
- Security validation

## Slide 14: Results

- Working MVP
- Role-specific dashboards
- Cloud-backed academic workflows
- Institutional branding for GDC Pulwama

## Slide 15: Future Scope

- Notifications
- Reports export
- Timetable
- Offline support
- Analytics

---

# Chapter 19: References

1. Android Developers Documentation, Android application fundamentals.
2. Firebase Documentation, Firebase Authentication.
3. Firebase Documentation, Cloud Firestore.
4. Firebase Documentation, Firestore Security Rules.
5. Cloudinary Documentation, Raw file upload API.
6. OkHttp Documentation.
7. Glide Documentation.
8. Material Components for Android Documentation.

---

# Chapter 20: Appendices

## Appendix A: Actual Source Areas Reviewed

- `app/src/main/java/com/example/campuscore`
- `app/src/main/res/layout`
- `app/src/main/res/menu`
- `app/src/main/res/values`
- `app/src/main/res/drawable`
- `app/src/main/AndroidManifest.xml`
- `app/build.gradle.kts`
- `gradle/libs.versions.toml`
- `firestore.rules`
- `storage.rules`

## Appendix B: Important Constants

- App name: CampusCore
- Institution: Government Degree College Pulwama (Boys)
- Short institution name: GDC Pulwama
- Roles: admin, teacher, student
- Attendance statuses: Present, Absent
- PDF MIME type: application/pdf
- PDF limit: 10 MB
- Updates auto-refresh interval: 10 minutes
- Maximum feed items: 80

## Appendix C: Evidence-Based Implementation Notes

This report does not claim implementation of modules not observed in the codebase. The documented features are derived from Java classes, XML resources, Gradle files, Firebase rules, repositories, models, adapters, fragments, activities, and utility classes present in the project.

