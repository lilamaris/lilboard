package com.lilamaris.lilboard.board.application.port.out;

import com.lilamaris.lilboard.board.domain.Comment;

public interface CommentSavePort {
    Comment save(Comment comment);
}
