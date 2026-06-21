create table if not exists public.expenses (
  id uuid primary key,
  user_id uuid not null references auth.users(id) on delete cascade,
  amount_cents bigint not null,
  merchant text not null,
  category text not null,
  occurred_at bigint not null,
  note text not null default '',
  custom_category text not null default '',
  payment_account text not null default '',
  booking_text text not null default '',
  purpose text not null default '',
  tags text not null default '',
  source_type text not null default '',
  source_hash text not null default '',
  imported_at bigint,
  created_at bigint not null,
  updated_at bigint not null,
  deleted_at bigint,
  sync_state text not null default 'PendingUpsert'
);

create table if not exists public.products (
  id uuid primary key,
  user_id uuid not null references auth.users(id) on delete cascade,
  name text not null,
  manufacturer text not null default '',
  purchase_price_cents bigint not null,
  purchased_at bigint not null,
  category text not null,
  note text not null default '',
  custom_category text not null default '',
  model_name text not null default '',
  serial_reference text not null default '',
  usage_duration_months integer not null default 0,
  warranty_until bigint,
  created_at bigint not null,
  updated_at bigint not null,
  deleted_at bigint,
  sync_state text not null default 'PendingUpsert'
);

create table if not exists public.vehicles (
  id uuid primary key,
  user_id uuid not null references auth.users(id) on delete cascade,
  display_name text not null,
  manufacturer text not null default '',
  model_name text not null default '',
  license_plate text not null default '',
  fuel_type text not null default 'Other',
  created_at bigint not null,
  updated_at bigint not null,
  deleted_at bigint,
  sync_state text not null default 'PendingUpsert'
);

create table if not exists public.trips (
  id uuid primary key,
  user_id uuid not null references auth.users(id) on delete cascade,
  vehicle_id uuid not null,
  date_at bigint not null,
  start_odometer_km bigint not null,
  end_odometer_km bigint not null,
  purpose text not null,
  start_location text not null default '',
  end_location text not null default '',
  note text not null default '',
  created_at bigint not null,
  updated_at bigint not null,
  deleted_at bigint,
  sync_state text not null default 'PendingUpsert'
);

create table if not exists public.fuel_entries (
  id uuid primary key,
  user_id uuid not null references auth.users(id) on delete cascade,
  vehicle_id uuid not null,
  date_at bigint not null,
  odometer_km bigint not null,
  distance_km bigint not null default 0,
  liters double precision not null,
  amount_cents bigint not null,
  fuel_station text not null default '',
  fuel_type_label text not null default '',
  note text not null default '',
  source_type text not null default '',
  source_hash text not null default '',
  created_at bigint not null,
  updated_at bigint not null,
  deleted_at bigint,
  sync_state text not null default 'PendingUpsert'
);

create table if not exists public.maintenance_items (
  id uuid primary key,
  user_id uuid not null references auth.users(id) on delete cascade,
  vehicle_id uuid not null,
  name text not null,
  interval_km bigint not null default 0,
  interval_months integer not null default 0,
  last_service_date bigint,
  last_service_odometer_km bigint,
  next_due_date bigint,
  next_due_odometer_km bigint,
  stock_quantity integer not null default 0,
  note text not null default '',
  created_at bigint not null,
  updated_at bigint not null,
  deleted_at bigint,
  sync_state text not null default 'PendingUpsert'
);

create table if not exists public.categories (
  id uuid primary key,
  user_id uuid not null references auth.users(id) on delete cascade,
  scope text not null,
  label text not null,
  created_at bigint not null,
  updated_at bigint not null,
  deleted_at bigint,
  sync_state text not null default 'PendingUpsert',
  unique (user_id, scope, label)
);

create table if not exists public.user_profiles (
  id uuid primary key,
  user_id uuid not null references auth.users(id) on delete cascade,
  email text not null default '',
  display_name text not null default '',
  notifications_enabled boolean not null default false,
  created_at bigint not null,
  updated_at bigint not null,
  sync_state text not null default 'PendingUpsert'
);

alter table public.expenses enable row level security;
alter table public.products enable row level security;
alter table public.vehicles enable row level security;
alter table public.trips enable row level security;
alter table public.fuel_entries enable row level security;
alter table public.maintenance_items enable row level security;
alter table public.categories enable row level security;
alter table public.user_profiles enable row level security;

alter table public.expenses force row level security;
alter table public.products force row level security;
alter table public.vehicles force row level security;
alter table public.trips force row level security;
alter table public.fuel_entries force row level security;
alter table public.maintenance_items force row level security;
alter table public.categories force row level security;
alter table public.user_profiles force row level security;

drop policy if exists "expenses own rows" on public.expenses;
drop policy if exists "products own rows" on public.products;
drop policy if exists "vehicles own rows" on public.vehicles;
drop policy if exists "trips own rows" on public.trips;
drop policy if exists "fuel entries own rows" on public.fuel_entries;
drop policy if exists "maintenance own rows" on public.maintenance_items;
drop policy if exists "categories own rows" on public.categories;
drop policy if exists "profiles own rows" on public.user_profiles;

create policy "expenses own rows" on public.expenses for all using (user_id = auth.uid()) with check (user_id = auth.uid());
create policy "products own rows" on public.products for all using (user_id = auth.uid()) with check (user_id = auth.uid());
create policy "vehicles own rows" on public.vehicles for all using (user_id = auth.uid()) with check (user_id = auth.uid());
create policy "trips own rows" on public.trips for all using (user_id = auth.uid()) with check (user_id = auth.uid());
create policy "fuel entries own rows" on public.fuel_entries for all using (user_id = auth.uid()) with check (user_id = auth.uid());
create policy "maintenance own rows" on public.maintenance_items for all using (user_id = auth.uid()) with check (user_id = auth.uid());
create policy "categories own rows" on public.categories for all using (user_id = auth.uid()) with check (user_id = auth.uid());
create policy "profiles own rows" on public.user_profiles for all using (user_id = auth.uid()) with check (user_id = auth.uid());

revoke all on public.expenses from anon;
revoke all on public.products from anon;
revoke all on public.vehicles from anon;
revoke all on public.trips from anon;
revoke all on public.fuel_entries from anon;
revoke all on public.maintenance_items from anon;
revoke all on public.categories from anon;
revoke all on public.user_profiles from anon;

grant select, insert, update, delete on public.expenses to authenticated;
grant select, insert, update, delete on public.products to authenticated;
grant select, insert, update, delete on public.vehicles to authenticated;
grant select, insert, update, delete on public.trips to authenticated;
grant select, insert, update, delete on public.fuel_entries to authenticated;
grant select, insert, update, delete on public.maintenance_items to authenticated;
grant select, insert, update, delete on public.categories to authenticated;
grant select, insert, update, delete on public.user_profiles to authenticated;
