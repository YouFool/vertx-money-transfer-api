/*
* Creates the two main entities: account and transaction
*/
create table account
(
    id uuid,
    balance decimal(19,4) default 0 not null,
    constraint account_pk
        primary key (id)
);

create table transaction
(
	id uuid,
	"from" uuid not null,
	to uuid not null,
	amount decimal(19,4) not null,
	constraint transaction_pk
		primary key (id),
	constraint transaction_account_from__fk
		foreign key ("from") references account (id),
	constraint transaction_account_to_fk
		foreign key (to) references account (id)
);

INSERT INTO account VALUES ( 'e6908ec0-1b70-4982-9362-8e9bdabbbd97', 10000.00);
INSERT INTO account VALUES ( 'f4e05ee5-12eb-4ae0-92c7-2cb4b6cd8ce2', 9.99);
INSERT INTO account VALUES ( '123e4567-e89b-12d3-a456-556642440000', 0.00);