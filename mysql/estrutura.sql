create table produto (
    id INT NOT NULL PRIMARY KEY,
    descricao VARCHAR(300) NOT NULL
);
insert into produto (id, descricao)
values (1, 'Arroz'),
    (2, 'Feijão'),
    (3, 'Óleo'),
    (4, 'Molho de Tomate'),
    (5, 'Açúcar'),
    (6, 'Sorvete'),
    (7, 'Sal refinado');
create table transacao (
    id_transacao INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    id_produto INT NOT NULL,
    id_ordem VARCHAR(100) NOT NULL,
    qt_transacao INT NOT NULL,
    in_revertida BIT NOT NULL DEFAULT(0),
    dt_criacao DATETIME NOT NULL,
    FOREIGN KEY (id_produto) REFERENCES produto(id),
    INDEX transacao_n1 (id_produto, in_revertida)
);
insert into transacao (id_produto, id_ordem, qt_transacao, dt_criacao)
values (1, 'entrada', 100, NOW()),
    (2, 'entrada', 100, NOW()),
    (3, 'entrada', 100, NOW()),
    (4, 'entrada', 100, NOW()),
    (5, 'entrada', 100, NOW()),
    (6, 'entrada', 100, NOW()),
    (7, 'entrada', 100, NOW());