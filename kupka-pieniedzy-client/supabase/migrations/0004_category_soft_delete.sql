alter table categories
  add column if not exists active boolean not null default true;

create index if not exists idx_categories_active
  on categories(user_id) where active;
