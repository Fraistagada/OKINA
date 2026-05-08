CREATE TABLE IF NOT EXISTS kanban_column (
    id    VARCHAR(20)  NOT NULL PRIMARY KEY,
    title VARCHAR(50)  NOT NULL,
    color VARCHAR(10)  NOT NULL,
    position INT       NOT NULL
);

CREATE TABLE IF NOT EXISTS task_type (
    id    VARCHAR(20)  NOT NULL PRIMARY KEY,
    label VARCHAR(50)  NOT NULL,
    color VARCHAR(10)  NOT NULL
);

CREATE TABLE IF NOT EXISTS app_user (
    id         VARCHAR(36)  NOT NULL PRIMARY KEY,
    pseudo     VARCHAR(50)  NOT NULL UNIQUE,
    email      VARCHAR(100) NOT NULL UNIQUE,
    password   VARCHAR(100) NOT NULL,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS board (
    id         VARCHAR(36) NOT NULL PRIMARY KEY,
    name       VARCHAR(100) NOT NULL,
    owner_id   VARCHAR(36)  NOT NULL,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (owner_id) REFERENCES app_user(id)
);

CREATE TABLE IF NOT EXISTS board_member (
    board_id VARCHAR(36) NOT NULL,
    user_id  VARCHAR(36) NOT NULL,
    PRIMARY KEY (board_id, user_id),
    FOREIGN KEY (board_id) REFERENCES board(id),
    FOREIGN KEY (user_id)  REFERENCES app_user(id)
);

CREATE TABLE IF NOT EXISTS task (
    id          VARCHAR(36)  NOT NULL PRIMARY KEY,
    board_id    VARCHAR(36)  NOT NULL,
    title       VARCHAR(200) NOT NULL,
    description CLOB,
    type_id     VARCHAR(20)  NOT NULL,
    column_id   VARCHAR(20)  NOT NULL,
    assignee_id VARCHAR(36),
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (board_id)    REFERENCES board(id),
    FOREIGN KEY (type_id)     REFERENCES task_type(id),
    FOREIGN KEY (column_id)   REFERENCES kanban_column(id),
    FOREIGN KEY (assignee_id) REFERENCES app_user(id)
);

CREATE TABLE IF NOT EXISTS comment (
    id         VARCHAR(36) NOT NULL PRIMARY KEY,
    task_id    VARCHAR(36) NOT NULL,
    author_id  VARCHAR(36) NOT NULL,
    content    CLOB        NOT NULL,
    created_at TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (task_id)   REFERENCES task(id),
    FOREIGN KEY (author_id) REFERENCES app_user(id)
);

CREATE TABLE IF NOT EXISTS attachment (
    id         VARCHAR(36)  NOT NULL PRIMARY KEY,
    task_id    VARCHAR(36)  NOT NULL,
    filename   VARCHAR(200) NOT NULL,
    file_size  BIGINT       NOT NULL,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (task_id) REFERENCES task(id)
);

CREATE TABLE IF NOT EXISTS task_history (
    id         VARCHAR(36) NOT NULL PRIMARY KEY,
    task_id    VARCHAR(36) NOT NULL,
    text       VARCHAR(300) NOT NULL,
    created_at TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (task_id) REFERENCES task(id)
);

MERGE INTO kanban_column (id, title, color, position) VALUES
    ('todo',  'À faire',  '#94a3b8', 1),
    ('doing', 'En cours', '#3b6ef5', 2),
    ('done',  'Terminé',  '#1f9c70', 3);

MERGE INTO task_type (id, label, color) VALUES
    ('standard',     'Standard',     '#6b7280'),
    ('bug',          'Bug',          '#e0394a'),
    ('spike',        'Spike',        '#b45ee0'),
    ('amelioration', 'Amélioration', '#1f9c70');