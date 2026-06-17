alter table receipts
  add column if not exists acknowledged boolean not null default false;
