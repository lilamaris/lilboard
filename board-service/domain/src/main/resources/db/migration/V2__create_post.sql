CREATE TABLE post (
    id UUID PRIMARY KEY,
    board_id UUID NOT NULL,
    title VARCHAR(300) NOT NULL,
    content VARCHAR(50000) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,

    CONSTRAINT fk_post_board_id FOREIGN KEY (board_id)
        REFERENCES board(id) ON DELETE RESTRICT ON UPDATE RESTRICT
);

CREATE INDEX idx_post_board_id ON post (board_id);
