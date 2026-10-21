package com.sqljudge.auth.service;

import com.sqljudge.auth.dto.request.AddUserNoteRequest;
import com.sqljudge.auth.dto.response.UserNoteResponse;
import com.sqljudge.auth.dto.response.UserNotesResponse;
import com.sqljudge.auth.entity.AdminUserNote;
import com.sqljudge.auth.entity.User;
import com.sqljudge.auth.repository.AdminUserNoteRepository;
import com.sqljudge.auth.repository.UserRepository;
import com.sqljudge.common.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserNoteService {

    private static final Limit LATEST_NOTES = Limit.of(200);

    private final UserRepository userRepository;
    private final AdminUserNoteRepository adminUserNoteRepository;

    @Transactional(readOnly = true)
    public UserNotesResponse list(String username) {
        User user = AdminUserLookup.findAny(userRepository, username);
        return new UserNotesResponse(
                adminUserNoteRepository.findLatestByUserId(user.getId(), LATEST_NOTES));
    }

    @Transactional
    public UserNoteResponse add(String username, AddUserNoteRequest request, CurrentUser author) {
        User user = AdminUserLookup.findLive(userRepository, username);
        AdminUserNote note =
                adminUserNoteRepository.save(AdminUserNote.of(user, author.id(), request.body()));
        log.info(
                "admin_user_note_added noteId={} userId={} actorId={}",
                note.getId(),
                user.getId(),
                author.id());
        return new UserNoteResponse(
                note.getId(), author.username(), note.getBody(), note.getCreatedAt());
    }
}
