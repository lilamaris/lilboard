CREATE TABLE comment (
    id UUID PRIMARY KEY,
    post_id UUID NOT NULL,
    content VARCHAR(5000) NOT NULL,
    created_at timestamptz NOT NULL,
    updated_at timestamptz,
    deleted_at timestamptz,

    CONSTRAINT fk_comment_post_id FOREIGN KEY (post_id)
        REFERENCES post(id) ON DELETE RESTRICT ON UPDATE RESTRICT
);

CREATE INDEX idx_comment_post_id ON comment (post_id);
