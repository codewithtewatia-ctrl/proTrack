<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.util.List" %>
<%@ page import="com.protrack.model.User" %>
<%@ page import="com.protrack.model.ActivityLog" %>
<%@ page import="com.protrack.util.DateUtil" %>
<%@ page import="com.protrack.util.ValidationUtil" %>
<%
    User profileUser = (User) session.getAttribute("currentUser");
    request.setAttribute("pageTitle", "User Profile");
    request.setAttribute("pageSubtitle", "Manage personal account credentials and review account security");
    request.setAttribute("activeMenu", "profile");

    @SuppressWarnings("unchecked")
    List<ActivityLog> myActivities = (List<ActivityLog>) request.getAttribute("myActivities");
%>

<jsp:include page="common/header.jsp" />
<jsp:include page="common/sidebar.jsp" />

<div class="main-wrapper">
    <jsp:include page="common/navbar.jsp" />

    <div class="content-body">
        <jsp:include page="common/alerts.jsp" />

        <div class="row g-4">
            <!-- Left Column: User Profile Card & Information Form -->
            <div class="col-lg-6">
                <!-- Profile Overview Header -->
                <div class="protrack-card mb-4">
                    <div class="protrack-card-body p-4 text-center">
                        <!-- Clickable Avatar: Clicking directly opens options to change or delete photo -->
                        <div class="avatar-interactive-wrapper position-relative d-inline-block mb-3"
                             role="button"
                             tabindex="0"
                             data-bs-toggle="modal"
                             data-bs-target="#avatarModal"
                             title="Click to change or delete profile picture"
                             style="cursor: pointer; border-radius: 50%;">
                            <% if (profileUser != null && profileUser.hasProfilePicture()) { %>
                                <img src="<%= ValidationUtil.escapeXml(profileUser.getProfilePicture()) %>"
                                     alt="Profile Picture" id="currentAvatarImg"
                                     class="rounded-circle object-fit-cover shadow-sm"
                                     style="width: 88px; height: 88px; border: 3px solid #6366f1 !important; display: block;">
                            <% } else { %>
                                <div class="rounded-circle bg-primary text-white fw-bold d-inline-flex align-items-center justify-content-center"
                                     id="currentAvatarInitial"
                                     style="width: 88px; height: 88px; font-size: 2.2rem; box-shadow: 0 4px 12px rgba(99, 102, 241, 0.35);">
                                    <%= profileUser != null && !profileUser.getFullName().isEmpty() ? ValidationUtil.escapeXml(profileUser.getFullName().substring(0, 1).toUpperCase()) : "U" %>
                                </div>
                            <% } %>

                            <!-- Hover Overlay with Edit indication -->
                            <div class="avatar-hover-overlay rounded-circle position-absolute top-0 start-0 w-100 h-100 d-flex flex-column align-items-center justify-content-center text-white"
                                 style="background: rgba(15, 23, 42, 0.55); backdrop-filter: blur(2px); opacity: 0; transition: opacity 0.2s ease-in-out;">
                                <i class="bi bi-camera-fill" style="font-size: 1.25rem;"></i>
                                <span style="font-size: 0.68rem; font-weight: 600; text-transform: uppercase; letter-spacing: 0.5px;">Edit</span>
                            </div>

                            <!-- Small camera indicator badge -->
                            <span class="position-absolute bottom-0 end-0 bg-primary text-white rounded-circle shadow-sm d-flex align-items-center justify-content-center border border-2 border-white"
                                  style="width: 26px; height: 26px; font-size: 0.75rem; pointer-events: none;">
                                <i class="bi bi-camera-fill"></i>
                            </span>
                        </div>

                        <h4 class="fw-bold text-dark mb-1"><%= profileUser != null ? ValidationUtil.escapeXml(profileUser.getFullName()) : "" %></h4>
                        <div class="text-secondary small mb-2"><%= profileUser != null ? ValidationUtil.escapeXml(profileUser.getEmail()) : "" %></div>
                        <span class="badge badge-custom badge-role-<%= profileUser != null ? profileUser.getRole().name() : "" %>">
                            <i class="bi bi-shield-check"></i> <%= profileUser != null ? profileUser.getRole().getDisplayName() : "" %>
                        </span>
                        <div class="text-muted small mt-3">
                            Account ID: #<%= profileUser != null ? profileUser.getId() : "" %> &bull; Status:
                            <span class="text-success fw-medium">Active</span>
                        </div>
                    </div>
                </div>

                <!-- Update Name & Email Form -->
                <div class="protrack-card">
                    <div class="protrack-card-header">
                        <h5 class="protrack-card-title">
                            <i class="bi bi-person-gear"></i> Personal Details
                        </h5>
                    </div>
                    <div class="protrack-card-body p-4">
                        <form action="<%= request.getContextPath() %>/profile" method="POST">
                            <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
                            <input type="hidden" name="action" value="updateProfile">
                            <div class="mb-3">
                                <label for="profName" class="form-label">Full Name</label>
                                <input type="text" class="form-control" id="profName" name="fullName"
                                       value="<%= profileUser != null ? ValidationUtil.escapeXml(profileUser.getFullName()) : "" %>" required>
                            </div>
                            <div class="mb-3">
                                <label for="profEmail" class="form-label">Email Address</label>
                                <input type="email" class="form-control" id="profEmail" name="email"
                                       value="<%= profileUser != null ? ValidationUtil.escapeXml(profileUser.getEmail()) : "" %>" required>
                            </div>
                            <div class="d-flex justify-content-end">
                                <button type="submit" class="btn btn-primary">
                                    <i class="bi bi-save me-1"></i> Update Details
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            </div>

            <!-- Right Column: Security (Change Password) & Recent History -->
            <div class="col-lg-6">
                <!-- Change Password Form -->
                <div class="protrack-card mb-4">
                    <div class="protrack-card-header">
                        <h5 class="protrack-card-title">
                            <i class="bi bi-shield-lock"></i> Security & Password
                        </h5>
                    </div>
                    <div class="protrack-card-body p-4">
                        <form action="<%= request.getContextPath() %>/profile" method="POST">
                            <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
                            <input type="hidden" name="action" value="changePassword">
                            <div class="mb-3">
                                <label for="currPass" class="form-label">Current Password</label>
                                <input type="password" class="form-control" id="currPass" name="currentPassword" required>
                            </div>
                            <div class="mb-3">
                                <label for="newPass" class="form-label">New Password</label>
                                <input type="password" class="form-control" id="newPass" name="newPassword" minlength="8" placeholder="Min 8 characters" required>
                            </div>
                            <div class="mb-4">
                                <label for="confPass" class="form-label">Confirm New Password</label>
                                <input type="password" class="form-control" id="confPass" name="confirmPassword" minlength="8" required>
                            </div>
                            <div class="d-flex justify-content-end">
                                <button type="submit" class="btn btn-primary">
                                    <i class="bi bi-key me-1"></i> Update Password
                                </button>
                            </div>
                        </form>
                    </div>
                </div>

                <!-- Recent Activity Timeline -->
                <div class="protrack-card">
                    <div class="protrack-card-header">
                        <h5 class="protrack-card-title">
                            <i class="bi bi-clock-history"></i> My Recent Audit Trail
                        </h5>
                    </div>
                    <div class="protrack-card-body">
                        <% if (myActivities != null && !myActivities.isEmpty()) { %>
                        <div class="timeline">
                            <% for (ActivityLog act : myActivities) { %>
                            <div class="timeline-item">
                                <div class="timeline-marker"></div>
                                <div class="timeline-content">
                                    <div class="timeline-title"><%= ValidationUtil.escapeXml(act.getDetails()) %></div>
                                    <div class="timeline-time">
                                        <i class="bi bi-clock me-1"></i><%= act.getFormattedCreatedAt() %>
                                    </div>
                                </div>
                            </div>
                            <% } %>
                        </div>
                        <% } else { %>
                        <p class="text-muted small text-center my-3">No activity logs recorded yet.</p>
                        <% } %>
                    </div>
                </div>
            </div>
        </div>

    </div><!-- /.content-body -->

<style>
.avatar-interactive-wrapper {
    transition: transform 0.2s cubic-bezier(0.4, 0, 0.2, 1);
}
.avatar-interactive-wrapper:hover {
    transform: scale(1.04);
}
.avatar-interactive-wrapper:hover .avatar-hover-overlay,
.avatar-interactive-wrapper:focus-visible .avatar-hover-overlay {
    opacity: 1 !important;
}
.avatar-action-btn {
    transition: all 0.2s ease;
    border-radius: 8px;
}
.avatar-action-btn:hover {
    transform: translateY(-1px);
}
</style>

<!-- Modal: Profile Picture Options (Triggered when user clicks on avatar) -->
<div class="modal fade" id="avatarModal" tabindex="-1" aria-labelledby="avatarModalLabel" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered" style="max-width: 400px;">
        <div class="modal-content border-0 shadow-lg rounded-4">
            <div class="modal-header border-0 pb-0">
                <h6 class="modal-title fw-bold text-dark" id="avatarModalLabel">
                    <i class="bi bi-person-bounding-box text-primary me-2"></i>Profile Picture
                </h6>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
            </div>
            <div class="modal-body p-4 text-center">
                <!-- Large Avatar Preview -->
                <div class="mb-3 position-relative d-inline-block">
                    <% if (profileUser != null && profileUser.hasProfilePicture()) { %>
                        <img id="avatarPreview" src="<%= ValidationUtil.escapeXml(profileUser.getProfilePicture()) %>" 
                             alt="Profile Picture" class="rounded-circle object-fit-cover shadow border" 
                             style="width: 108px; height: 108px; border: 3px solid #6366f1 !important;">
                    <% } else { %>
                        <div id="avatarPreviewInitial" class="rounded-circle bg-primary text-white fw-bold d-inline-flex align-items-center justify-content-center shadow"
                             style="width: 108px; height: 108px; font-size: 2.8rem;">
                            <%= profileUser != null && !profileUser.getFullName().isEmpty() ? ValidationUtil.escapeXml(profileUser.getFullName().substring(0, 1).toUpperCase()) : "U" %>
                        </div>
                        <img id="avatarPreview" src="" alt="Profile Picture" class="rounded-circle object-fit-cover shadow border d-none" 
                             style="width: 108px; height: 108px; border: 3px solid #6366f1 !important;">
                    <% } %>
                </div>

                <div class="mb-3">
                    <h6 class="fw-bold mb-1 text-dark"><%= profileUser != null ? ValidationUtil.escapeXml(profileUser.getFullName()) : "User" %></h6>
                    <p class="text-muted small mb-0">
                        <%= (profileUser != null && profileUser.hasProfilePicture()) ? "Choose an option to change or delete your photo" : "Choose an option to add a profile photo" %>
                    </p>
                </div>

                <form id="avatarForm" action="<%= request.getContextPath() %>/profile" method="POST">
                    <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
                    <input type="hidden" id="avatarAction" name="action" value="updateProfilePicture">
                    <input type="hidden" id="profilePictureData" name="profilePicture" value="<%= profileUser != null && profileUser.hasProfilePicture() ? ValidationUtil.escapeXml(profileUser.getProfilePicture()) : "" %>">

                    <!-- Hidden file picker -->
                    <input type="file" id="avatarFileInput" class="d-none" accept="image/png, image/jpeg, image/jpg, image/webp" onchange="handleAvatarFileSelect(this)">

                    <!-- Direct Action Options: Change Image or Delete Image -->
                    <div id="avatarOptionsBlock" class="d-grid gap-2">
                        <!-- Option 1: Change Image / Upload Image -->
                        <button type="button" class="btn btn-primary avatar-action-btn py-2.5 d-flex align-items-center justify-content-center gap-2 fw-semibold"
                                onclick="document.getElementById('avatarFileInput').click()">
                            <i class="bi bi-cloud-arrow-up-fill fs-5"></i>
                            <span><%= (profileUser != null && profileUser.hasProfilePicture()) ? "Change Image" : "Upload Image" %></span>
                        </button>

                        <% if (profileUser != null && profileUser.hasProfilePicture()) { %>
                        <!-- Option 2: Delete Image -->
                        <button type="button" class="btn btn-outline-danger avatar-action-btn py-2.5 d-flex align-items-center justify-content-center gap-2 fw-semibold"
                                onclick="submitRemoveAvatar()">
                            <i class="bi bi-trash3-fill fs-5"></i>
                            <span>Delete Image</span>
                        </button>
                        <% } %>

                        <!-- Option: Image URL toggle -->
                        <button type="button" class="btn btn-link btn-sm text-secondary text-decoration-none mt-1" onclick="toggleUrlSection()">
                            <i class="bi bi-link-45deg me-1"></i> <span id="urlToggleLabel">Or enter image URL</span>
                        </button>
                    </div>

                    <!-- Collapsible URL Input Field -->
                    <div id="avatarUrlSection" class="text-start p-3 bg-light rounded-3 mt-2 d-none">
                        <label class="form-label small fw-semibold text-dark mb-1">Image Web Address (URL)</label>
                        <div class="input-group input-group-sm mb-1">
                            <input type="url" id="avatarUrlInput" class="form-control" placeholder="https://example.com/avatar.jpg">
                            <button class="btn btn-primary" type="button" onclick="applyAvatarUrl()">Preview</button>
                        </div>
                        <div class="form-text" style="font-size: 0.72rem;">Enter a direct image link (JPG, PNG, WEBP).</div>
                    </div>

                    <!-- Pending Save Confirmation -->
                    <div id="avatarPendingActions" class="p-3 bg-light rounded-3 border mt-3 d-none">
                        <div class="d-flex align-items-center gap-2 text-success small fw-medium mb-3 justify-content-center">
                            <i class="bi bi-check-circle-fill"></i> New photo previewed! Save changes?
                        </div>
                        <div class="d-flex gap-2 justify-content-center">
                            <button type="button" class="btn btn-sm btn-outline-secondary px-3" onclick="cancelAvatarPreview()">
                                Cancel
                            </button>
                            <button type="submit" class="btn btn-sm btn-success px-4 fw-semibold">
                                <i class="bi bi-check-lg me-1"></i> Save Photo
                            </button>
                        </div>
                    </div>
                </form>
            </div>
        </div>
    </div>
</div>

<script>
    const originalAvatarSrc = "<%= profileUser != null && profileUser.hasProfilePicture() ? ValidationUtil.escapeXml(profileUser.getProfilePicture()) : "" %>";
    const hasInitialAvatar = <%= profileUser != null && !profileUser.hasProfilePicture() %>;

    function handleAvatarFileSelect(input) {
        if (input.files && input.files[0]) {
            const file = input.files[0];
            if (file.size > 2 * 1024 * 1024) {
                alert('Image size exceeds 2MB limit. Please choose a smaller image.');
                input.value = '';
                return;
            }
            const reader = new FileReader();
            reader.onload = function(e) {
                const dataUrl = e.target.result;
                displayAvatarPreview(dataUrl);
                document.getElementById('profilePictureData').value = dataUrl;
                showPendingSave();
            };
            reader.readAsDataURL(file);
        }
    }

    function toggleUrlSection() {
        const sec = document.getElementById('avatarUrlSection');
        if (sec.classList.contains('d-none')) {
            sec.classList.remove('d-none');
            document.getElementById('avatarUrlInput').focus();
        } else {
            sec.classList.add('d-none');
        }
    }

    function applyAvatarUrl() {
        const url = document.getElementById('avatarUrlInput').value.trim();
        if (!url) {
            alert('Please enter a valid image URL.');
            return;
        }
        displayAvatarPreview(url);
        document.getElementById('profilePictureData').value = url;
        showPendingSave();
    }

    function displayAvatarPreview(src) {
        const previewImg = document.getElementById('avatarPreview');
        previewImg.src = src;
        previewImg.classList.remove('d-none');
        const initialEl = document.getElementById('avatarPreviewInitial');
        if (initialEl) {
            initialEl.classList.add('d-none');
        }
    }

    function showPendingSave() {
        document.getElementById('avatarOptionsBlock').classList.add('d-none');
        document.getElementById('avatarUrlSection').classList.add('d-none');
        document.getElementById('avatarPendingActions').classList.remove('d-none');
    }

    function cancelAvatarPreview() {
        document.getElementById('avatarFileInput').value = '';
        document.getElementById('avatarUrlInput').value = '';
        document.getElementById('profilePictureData').value = originalAvatarSrc;

        if (originalAvatarSrc) {
            displayAvatarPreview(originalAvatarSrc);
        } else if (hasInitialAvatar) {
            const previewImg = document.getElementById('avatarPreview');
            previewImg.classList.add('d-none');
            previewImg.src = '';
            const initialEl = document.getElementById('avatarPreviewInitial');
            if (initialEl) initialEl.classList.remove('d-none');
        }

        document.getElementById('avatarPendingActions').classList.add('d-none');
        document.getElementById('avatarOptionsBlock').classList.remove('d-none');
    }

    function submitRemoveAvatar() {
        if (confirm('Are you sure you want to delete your profile picture?')) {
            document.getElementById('avatarAction').value = 'removeProfilePicture';
            document.getElementById('profilePictureData').value = '';
            document.getElementById('avatarForm').submit();
        }
    }
</script>

<jsp:include page="common/footer.jsp" />
