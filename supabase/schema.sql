-- =====================================================================
-- Flashly: esquema de la base de datos en Supabase (PostgreSQL)
-- Tablas, relaciones, políticas RLS y funciones de backend (crear mazo y partidas).
-- Se ejecuta una sola vez en el SQL Editor del proyecto (o como migración).
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1. Tablas
-- ---------------------------------------------------------------------

-- Mazos de preguntas. El creador se toma de la sesión (auth.uid()).
create table public.mazos (
    id          uuid primary key default gen_random_uuid(),
    nombre      text not null check (char_length(btrim(nombre)) between 1 and 60),
    descripcion text check (descripcion is null or char_length(descripcion) <= 200),
    creador_id  uuid not null default auth.uid() references auth.users (id) on delete cascade,
    es_publico  boolean not null default false,
    created_at  timestamptz not null default now()
);

-- Preguntas de selección múltiple (siempre 4 opciones). No guardan la respuesta correcta.
create table public.preguntas (
    id         uuid primary key default gen_random_uuid(),
    mazo_id    uuid not null references public.mazos (id) on delete cascade,
    enunciado  text not null check (char_length(btrim(enunciado)) between 1 and 300),
    opciones   text[] not null check (cardinality(opciones) = 4),
    created_at timestamptz not null default now()
);

-- Respuesta correcta de cada pregunta, separada para que la app nunca pueda leerla.
create table public.respuestas_correctas (
    pregunta_id     uuid primary key references public.preguntas (id) on delete cascade,
    indice_correcto int not null check (indice_correcto between 0 and 3)
);

-- Partidas y su puntuación. Solo las escriben las funciones iniciar_partida() y
-- responder(), de modo que la puntuación la calcula el servidor y no el teléfono.
create table public.resultados (
    id              uuid primary key default gen_random_uuid(),
    usuario_id      uuid not null default auth.uid() references auth.users (id) on delete cascade,
    mazo_id         uuid not null references public.mazos (id) on delete cascade,
    puntuacion      int not null default 0 check (puntuacion >= 0),
    total_preguntas int not null check (total_preguntas > 0),
    terminada       boolean not null default false,
    created_at      timestamptz not null default now(),
    check (puntuacion <= total_preguntas)
);

-- Respuesta dada en cada pregunta de una partida. La llave primaria impide contestar
-- dos veces la misma pregunta (y así probar opción por opción hasta dar con la correcta).
create table public.respuestas_partida (
    resultado_id uuid not null references public.resultados (id) on delete cascade,
    pregunta_id  uuid not null references public.preguntas (id) on delete cascade,
    indice       int not null check (indice between 0 and 3),
    correcta     boolean not null,
    created_at   timestamptz not null default now(),
    primary key (resultado_id, pregunta_id)
);

-- Índices sobre las llaves foráneas y los filtros que usa la app
create index mazos_creador_id_idx on public.mazos (creador_id);
create index mazos_publicos_idx on public.mazos (created_at desc) where es_publico;
create index preguntas_mazo_id_idx on public.preguntas (mazo_id);
create index resultados_usuario_id_idx on public.resultados (usuario_id, created_at desc);
create index resultados_mazo_id_idx on public.resultados (mazo_id);
create index respuestas_partida_pregunta_id_idx on public.respuestas_partida (pregunta_id);

-- ---------------------------------------------------------------------
-- 2. Permisos de la API: solo usuarios con sesión, y solo lo que la app necesita
-- ---------------------------------------------------------------------

revoke all on public.mazos, public.preguntas, public.respuestas_correctas, public.resultados,
    public.respuestas_partida
    from anon, authenticated;

grant select, insert, update, delete on public.mazos to authenticated;
grant select, insert, update, delete on public.preguntas to authenticated;
grant insert on public.respuestas_correctas to authenticated;   -- sin select: nunca se lee
grant select on public.resultados to authenticated;             -- se escriben solo desde las funciones
-- respuestas_partida: sin permisos directos, solo se usa dentro de responder()

-- ---------------------------------------------------------------------
-- 3. Row Level Security
-- ---------------------------------------------------------------------

alter table public.mazos enable row level security;
alter table public.preguntas enable row level security;
alter table public.respuestas_correctas enable row level security;
alter table public.resultados enable row level security;
alter table public.respuestas_partida enable row level security;

-- Mazos: se ven los propios y los públicos; solo el creador los modifica
create policy "Ver mazos propios o públicos" on public.mazos
    for select to authenticated
    using (es_publico or creador_id = (select auth.uid()));

create policy "Crear mazos propios" on public.mazos
    for insert to authenticated
    with check (creador_id = (select auth.uid()));

create policy "Editar mazos propios" on public.mazos
    for update to authenticated
    using (creador_id = (select auth.uid()))
    with check (creador_id = (select auth.uid()));

create policy "Eliminar mazos propios" on public.mazos
    for delete to authenticated
    using (creador_id = (select auth.uid()));

-- Preguntas: visibles si su mazo es visible; solo el creador del mazo las modifica
create policy "Ver preguntas de mazos visibles" on public.preguntas
    for select to authenticated
    using (exists (
        select 1 from public.mazos m
        where m.id = mazo_id and (m.es_publico or m.creador_id = (select auth.uid()))
    ));

create policy "Crear preguntas en mazos propios" on public.preguntas
    for insert to authenticated
    with check (exists (
        select 1 from public.mazos m
        where m.id = mazo_id and m.creador_id = (select auth.uid())
    ));

create policy "Editar preguntas de mazos propios" on public.preguntas
    for update to authenticated
    using (exists (
        select 1 from public.mazos m
        where m.id = mazo_id and m.creador_id = (select auth.uid())
    ))
    with check (exists (
        select 1 from public.mazos m
        where m.id = mazo_id and m.creador_id = (select auth.uid())
    ));

create policy "Eliminar preguntas de mazos propios" on public.preguntas
    for delete to authenticated
    using (exists (
        select 1 from public.mazos m
        where m.id = mazo_id and m.creador_id = (select auth.uid())
    ));

-- Respuestas correctas: solo se registran al crear el mazo. No hay política de lectura,
-- así que ningún usuario puede consultarlas; se comprueban con validar_respuesta().
create policy "Registrar respuestas de preguntas propias" on public.respuestas_correctas
    for insert to authenticated
    with check (exists (
        select 1 from public.preguntas p
        join public.mazos m on m.id = p.mazo_id
        where p.id = pregunta_id and m.creador_id = (select auth.uid())
    ));

-- Resultados: cada usuario ve solo los suyos. No hay políticas de escritura: las
-- partidas las crean y actualizan iniciar_partida() y responder().
create policy "Ver resultados propios" on public.resultados
    for select to authenticated
    using (usuario_id = (select auth.uid()));

-- Respuestas de partida: sin políticas, nadie las consulta ni modifica directamente.

-- ---------------------------------------------------------------------
-- 4. Funciones de backend (se llaman desde la app por RPC)
-- ---------------------------------------------------------------------

-- Crea el mazo con sus preguntas y respuestas en una sola transacción: si algo falla
-- (datos inválidos, corte de conexión) no queda nada a medias. Se ejecuta con los
-- permisos del usuario (security invoker), así que las políticas RLS siguen aplicando.
-- p_preguntas: [{"enunciado": "...", "opciones": ["a","b","c","d"], "correcta": 0..3}, ...]
create function public.crear_mazo(
    p_nombre text, p_descripcion text, p_es_publico boolean, p_preguntas jsonb)
returns uuid
language plpgsql
security invoker
set search_path = ''
as $$
declare
    v_mazo     uuid;
    v_pregunta uuid;
    p          jsonb;
begin
    if jsonb_typeof(p_preguntas) is distinct from 'array' or jsonb_array_length(p_preguntas) = 0 then
        raise exception 'El mazo necesita al menos una pregunta';
    end if;

    insert into public.mazos (nombre, descripcion, es_publico)
    values (btrim(p_nombre), nullif(btrim(p_descripcion), ''), p_es_publico)
    returning id into v_mazo;

    for p in select value from jsonb_array_elements(p_preguntas) loop
        if exists (select 1 from jsonb_array_elements_text(p -> 'opciones') o where btrim(o) = '')
           or (select count(distinct lower(btrim(o))) from jsonb_array_elements_text(p -> 'opciones') o) <> 4 then
            raise exception 'Cada pregunta necesita 4 opciones distintas y no vacías';
        end if;

        insert into public.preguntas (mazo_id, enunciado, opciones)
        values (v_mazo, btrim(p ->> 'enunciado'),
                array(select btrim(o) from jsonb_array_elements_text(p -> 'opciones') o))
        returning id into v_pregunta;

        insert into public.respuestas_correctas (pregunta_id, indice_correcto)
        values (v_pregunta, (p ->> 'correcta')::int);
    end loop;

    return v_mazo;
end;
$$;

-- Abre una partida del usuario en un mazo que puede ver y fija el total de preguntas.
-- Security definer porque la app no tiene permiso de escribir en resultados.
create function public.iniciar_partida(p_mazo_id uuid)
returns uuid
language plpgsql
security definer
set search_path = ''
as $$
declare
    v_uid   uuid := auth.uid();
    v_total int;
    v_id    uuid;
begin
    if v_uid is null then
        raise exception 'Debes iniciar sesión';
    end if;
    if not exists (
        select 1 from public.mazos m
        where m.id = p_mazo_id and (m.es_publico or m.creador_id = v_uid)
    ) then
        raise exception 'El mazo no existe o no tienes acceso a él';
    end if;

    select count(*) into v_total from public.preguntas where mazo_id = p_mazo_id;
    if v_total = 0 then
        raise exception 'Este mazo aún no tiene preguntas';
    end if;

    insert into public.resultados (usuario_id, mazo_id, total_preguntas)
    values (v_uid, p_mazo_id, v_total)
    returning id into v_id;
    return v_id;
end;
$$;

-- Registra la respuesta a una pregunta de la partida y suma el punto en el servidor.
-- Cada pregunta se contesta una sola vez: si se repite (por ejemplo, un reintento tras
-- un corte de red) devuelve lo ya registrado sin cambiar la puntuación. La partida se
-- marca como terminada al contestar la última pregunta.
-- Devuelve {"correcta": bool, "puntuacion": int}.
create function public.responder(p_partida_id uuid, p_pregunta_id uuid, p_indice int)
returns jsonb
language plpgsql
security definer
set search_path = ''
as $$
declare
    v_partida  public.resultados%rowtype;
    v_previa   boolean;
    v_correcta boolean;
begin
    select * into v_partida
    from public.resultados
    where id = p_partida_id and usuario_id = auth.uid()
    for update;   -- serializa respuestas simultáneas de la misma partida
    if not found then
        raise exception 'La partida no existe';
    end if;

    select correcta into v_previa
    from public.respuestas_partida
    where resultado_id = p_partida_id and pregunta_id = p_pregunta_id;
    if found then
        return jsonb_build_object('correcta', v_previa, 'puntuacion', v_partida.puntuacion);
    end if;

    if v_partida.terminada then
        raise exception 'La partida ya terminó';
    end if;
    if p_indice is null or p_indice not between 0 and 3 then
        raise exception 'Opción no válida';
    end if;

    select rc.indice_correcto = p_indice into v_correcta
    from public.respuestas_correctas rc
    join public.preguntas p on p.id = rc.pregunta_id
    where rc.pregunta_id = p_pregunta_id and p.mazo_id = v_partida.mazo_id;
    if v_correcta is null then
        raise exception 'La pregunta no pertenece a este mazo';
    end if;

    insert into public.respuestas_partida (resultado_id, pregunta_id, indice, correcta)
    values (p_partida_id, p_pregunta_id, p_indice, v_correcta);

    update public.resultados
    set puntuacion = puntuacion + case when v_correcta then 1 else 0 end,
        terminada  = (select count(*) from public.respuestas_partida
                      where resultado_id = p_partida_id) >= total_preguntas
    where id = p_partida_id
    returning puntuacion into v_partida.puntuacion;

    return jsonb_build_object('correcta', v_correcta, 'puntuacion', v_partida.puntuacion);
end;
$$;

revoke execute on function public.crear_mazo(text, text, boolean, jsonb) from public, anon;
revoke execute on function public.iniciar_partida(uuid) from public, anon;
revoke execute on function public.responder(uuid, uuid, int) from public, anon;
grant execute on function public.crear_mazo(text, text, boolean, jsonb) to authenticated;
grant execute on function public.iniciar_partida(uuid) to authenticated;
grant execute on function public.responder(uuid, uuid, int) to authenticated;

-- ---------------------------------------------------------------------
-- 5. Avatares predefinidos (Storage)
-- ---------------------------------------------------------------------

-- Bucket público: cualquiera puede ver las imágenes por su URL, pero sin políticas de
-- escritura nadie puede subir, cambiar ni borrar archivos desde la app.
insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values ('avatares', 'avatares', true, 1048576, array['image/png']);

-- Catálogo de avatares que el usuario puede elegir. Para agregar uno basta con subir la
-- imagen al bucket y añadir una fila aquí; la app no necesita una nueva versión.
create table public.avatares (
    id      text primary key,                                -- p. ej. 'avatar_1'
    archivo text not null unique,                            -- ruta dentro del bucket
    color   text not null check (color ~ '^#[0-9A-Fa-f]{6}$'), -- fondo pastel del círculo
    orden   int not null
);

revoke all on public.avatares from anon, authenticated;
grant select on public.avatares to authenticated;
alter table public.avatares enable row level security;

create policy "Ver catálogo de avatares" on public.avatares
    for select to authenticated
    using (true);

insert into public.avatares (id, archivo, color, orden) values
    ('avatar_1', 'avatar_1.png', '#F4A0B5', 1),
    ('avatar_2', 'avatar_2.png', '#BDE8D5', 2),
    ('avatar_3', 'avatar_3.png', '#C8C5FF', 3),
    ('avatar_4', 'avatar_4.png', '#FFB477', 4),
    ('avatar_5', 'avatar_5.png', '#F7D66D', 5),
    ('avatar_6', 'avatar_6.png', '#9299F5', 6),
    ('avatar_7', 'avatar_7.png', '#FBE3ED', 7);

-- ---------------------------------------------------------------------
-- 6. Perfiles de usuario
-- ---------------------------------------------------------------------

-- Supabase Auth guarda el correo y la contraseña; los datos adicionales viven aquí.
-- El nombre de usuario se guarda en minúsculas, así que el índice único evita duplicados
-- sin importar mayúsculas.
create table public.perfiles (
    id              uuid primary key references auth.users (id) on delete cascade,
    nombre_completo text not null check (char_length(btrim(nombre_completo)) between 2 and 80),
    nombre_usuario  text not null unique check (nombre_usuario ~ '^[a-z0-9_.]{3,20}$'),
    telefono        text check (
        telefono is null
        or (telefono ~ '^\+?[0-9 ]+$' and char_length(regexp_replace(telefono, '\D', '', 'g')) between 7 and 15)
    ),
    avatar_id       text not null references public.avatares (id),
    xp              int not null default 0 check (xp >= 0),
    created_at      timestamptz not null default now()
);

create index perfiles_avatar_id_idx on public.perfiles (avatar_id);

revoke all on public.perfiles from anon, authenticated;
grant select on public.perfiles to authenticated;
alter table public.perfiles enable row level security;

-- Cada usuario ve su propio perfil completo (teléfono y nombre son datos privados).
create policy "Ver perfil propio" on public.perfiles
    for select to authenticated
    using (id = (select auth.uid()));

-- Al registrarse, Auth crea la fila en auth.users y este trigger crea el perfil con los
-- datos enviados desde la app (user metadata) y un avatar al azar del catálogo.
create function public.crear_perfil_nuevo_usuario()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
declare
    m         jsonb := coalesce(new.raw_user_meta_data, '{}'::jsonb);
    v_usuario text  := lower(btrim(m ->> 'nombre_usuario'));
    v_avatar  text;
begin
    select id into v_avatar from public.avatares order by random() limit 1;

    if coalesce(v_usuario, '') = '' then
        -- Usuario creado fuera de la app (p. ej. desde el panel): perfil provisional editable
        insert into public.perfiles (id, nombre_completo, nombre_usuario, avatar_id)
        values (new.id, 'Usuario de Flashly', 'usuario_' || substr(replace(new.id::text, '-', ''), 1, 8), v_avatar);
    else
        if coalesce(btrim(m ->> 'telefono'), '') = '' then
            raise exception 'El teléfono es obligatorio';
        end if;
        insert into public.perfiles (id, nombre_completo, nombre_usuario, telefono, avatar_id)
        values (new.id, btrim(m ->> 'nombre_completo'), v_usuario, btrim(m ->> 'telefono'), v_avatar);
    end if;
    return new;
end;
$$;

revoke execute on function public.crear_perfil_nuevo_usuario() from public, anon, authenticated;

create trigger al_crear_usuario
    after insert on auth.users
    for each row execute function public.crear_perfil_nuevo_usuario();

-- Comprueba en vivo, desde el formulario de registro (aún sin sesión), si un nombre de
-- usuario está libre. Solo responde sí o no; no expone ningún otro dato del perfil.
create function public.nombre_usuario_disponible(p_nombre text)
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
    select not exists (select 1 from public.perfiles where nombre_usuario = lower(btrim(p_nombre)));
$$;

revoke execute on function public.nombre_usuario_disponible(text) from public;
grant execute on function public.nombre_usuario_disponible(text) to anon, authenticated;

-- Edición del perfil propio. El permiso es por columnas: la app puede cambiar nombre,
-- usuario, teléfono y avatar, pero no el XP (lo suman solo las funciones de juego).
grant update (nombre_completo, nombre_usuario, telefono, avatar_id) on public.perfiles to authenticated;

create policy "Editar perfil propio" on public.perfiles
    for update to authenticated
    using (id = (select auth.uid()))
    with check (id = (select auth.uid()));

-- ---------------------------------------------------------------------
-- 7. Mazos v2: categorías, tarjetas de dos tipos, claves ocultas y mazos guardados
-- ---------------------------------------------------------------------

-- Datos públicos de cada usuario (lo que otros pueden ver: creador de un mazo, ranking).
-- Se copia automáticamente desde perfiles; nombre completo y teléfono nunca salen de allí.
create table public.perfiles_publicos (
    id             uuid primary key references public.perfiles (id) on delete cascade,
    nombre_usuario text not null,
    avatar_id      text not null references public.avatares (id),
    xp             int not null default 0
);

create index perfiles_publicos_avatar_id_idx on public.perfiles_publicos (avatar_id);

revoke all on public.perfiles_publicos from anon, authenticated;
grant select on public.perfiles_publicos to authenticated;
alter table public.perfiles_publicos enable row level security;

create policy "Ver datos públicos de usuarios" on public.perfiles_publicos
    for select to authenticated
    using (true);

create function public.sincronizar_perfil_publico()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
    insert into public.perfiles_publicos (id, nombre_usuario, avatar_id, xp)
    values (new.id, new.nombre_usuario, new.avatar_id, new.xp)
    on conflict (id) do update
        set nombre_usuario = excluded.nombre_usuario, avatar_id = excluded.avatar_id, xp = excluded.xp;
    return new;
end;
$$;

revoke execute on function public.sincronizar_perfil_publico() from public, anon, authenticated;

create trigger al_cambiar_perfil
    after insert or update of nombre_usuario, avatar_id, xp on public.perfiles
    for each row execute function public.sincronizar_perfil_publico();

insert into public.perfiles_publicos (id, nombre_usuario, avatar_id, xp)
select id, nombre_usuario, avatar_id, xp from public.perfiles
on conflict (id) do nothing;

-- Categorías o temas de los mazos (filtros de Explorar)
create table public.categorias (
    id     text primary key,
    nombre text not null,
    icono  text not null,   -- nombre del ícono en la app
    color  text not null,   -- color de la paleta Flashly
    orden  int not null
);

revoke all on public.categorias from anon, authenticated;
grant select on public.categorias to authenticated;
alter table public.categorias enable row level security;

create policy "Ver categorías" on public.categorias
    for select to authenticated
    using (true);

insert into public.categorias (id, nombre, icono, color, orden) values
    ('idiomas',     'Idiomas',     'translate', 'LAVANDA',     1),
    ('ciencias',    'Ciencias',    'science',   'MENTA',       2),
    ('matematicas', 'Matemáticas', 'calculate', 'PERIWINKLE',  3),
    ('historia',    'Historia',    'history',   'MELOCOTON',   4),
    ('geografia',   'Geografía',   'public',    'MANTEQUILLA', 5),
    ('tecnologia',  'Tecnología',  'computer',  'LAVANDA',     6),
    ('arte',        'Arte',        'palette',   'ROSA',        7),
    ('otros',       'Otros',       'category',  'MENTA',       8);

-- Mazos: categoría, color de portada, fecha de última actividad y texto para búsqueda
alter table public.mazos
    add column categoria_id text not null default 'otros' references public.categorias (id),
    add column color        text not null default 'ROSA'
        check (color in ('ROSA', 'MENTA', 'LAVANDA', 'MELOCOTON', 'MANTEQUILLA', 'PERIWINKLE')),
    add column updated_at   timestamptz not null default now(),
    add column busqueda     tsvector generated always as (
        to_tsvector('spanish', coalesce(nombre, '') || ' ' || coalesce(descripcion, ''))
    ) stored,
    -- Segunda relación del creador, hacia sus datos públicos: permite mostrar "por @usuario"
    add constraint mazos_creador_publico_fkey foreign key (creador_id) references public.perfiles_publicos (id) on delete cascade;

create index mazos_categoria_id_idx on public.mazos (categoria_id);
create index mazos_busqueda_idx on public.mazos using gin (busqueda);

-- Tarjetas de dos tipos que se pueden mezclar en un mismo mazo:
--   opcion_multiple: enunciado + opciones (2 o más, sin máximo fijo)
--   definicion:      termino (anverso) + definicion (reverso)
create table public.tarjetas (
    id         uuid primary key default gen_random_uuid(),
    mazo_id    uuid not null references public.mazos (id) on delete cascade,
    tipo       text not null check (tipo in ('opcion_multiple', 'definicion')),
    posicion   int not null,
    enunciado  text,
    opciones   text[],
    termino    text,
    definicion text,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    check (
        (tipo = 'opcion_multiple'
            and char_length(btrim(enunciado)) between 1 and 300
            and cardinality(opciones) >= 2
            and termino is null and definicion is null)
        or
        (tipo = 'definicion'
            and char_length(btrim(termino)) between 1 and 120
            and char_length(btrim(definicion)) between 1 and 600
            and enunciado is null and opciones is null)
    )
);

create index tarjetas_mazo_id_idx on public.tarjetas (mazo_id, posicion);

-- Respuesta correcta y explicación de las preguntas: la app no puede leerlas ni escribirlas;
-- solo las usan la función guardar_mazo() y (en el paso 7) la de responder.
create table public.claves (
    tarjeta_id      uuid primary key references public.tarjetas (id) on delete cascade,
    indice_correcto int not null check (indice_correcto >= 0),
    explicacion     text check (explicacion is null or char_length(explicacion) <= 600)
);

-- Mazos públicos de otros usuarios guardados en la biblioteca personal
create table public.mazos_guardados (
    usuario_id uuid not null default auth.uid() references auth.users (id) on delete cascade,
    mazo_id    uuid not null references public.mazos (id) on delete cascade,
    created_at timestamptz not null default now(),
    primary key (usuario_id, mazo_id)
);

create index mazos_guardados_mazo_id_idx on public.mazos_guardados (mazo_id);

revoke all on public.tarjetas, public.claves, public.mazos_guardados from anon, authenticated;
grant select on public.tarjetas to authenticated;               -- se escriben solo con guardar_mazo()
grant select, insert, delete on public.mazos_guardados to authenticated;
-- claves: sin ningún permiso para la app

alter table public.tarjetas enable row level security;
alter table public.claves enable row level security;
alter table public.mazos_guardados enable row level security;

create policy "Ver tarjetas de mazos visibles" on public.tarjetas
    for select to authenticated
    using (exists (
        select 1 from public.mazos m
        where m.id = mazo_id and (m.es_publico or m.creador_id = (select auth.uid()))
    ));

create policy "Ver mis mazos guardados" on public.mazos_guardados
    for select to authenticated
    using (usuario_id = (select auth.uid()));

-- Solo se guardan mazos públicos de otros usuarios
create policy "Guardar mazos públicos ajenos" on public.mazos_guardados
    for insert to authenticated
    with check (
        usuario_id = (select auth.uid())
        and exists (
            select 1 from public.mazos m
            where m.id = mazo_id and m.es_publico and m.creador_id <> (select auth.uid())
        )
    );

create policy "Quitar mis mazos guardados" on public.mazos_guardados
    for delete to authenticated
    using (usuario_id = (select auth.uid()));

-- Crea (p_mazo_id = null) o edita un mazo con todas sus tarjetas en una sola transacción.
-- p_mazo:     {"nombre", "descripcion", "categoria_id", "color", "es_publico"}
-- p_tarjetas: [{"id"?, "tipo": "opcion_multiple", "enunciado", "opciones": [...], "correcta", "explicacion"?},
--              {"id"?, "tipo": "definicion", "termino", "definicion"}]
-- Al editar, las tarjetas que traen "id" se actualizan (conservan su historial), las nuevas se
-- crean y las que ya no vienen se eliminan. El orden del arreglo es el orden del mazo.
-- Security definer para escribir en claves; por eso comprueba a mano que el mazo sea del usuario.
create function public.guardar_mazo(p_mazo_id uuid, p_mazo jsonb, p_tarjetas jsonb)
returns uuid
language plpgsql
security definer
set search_path = ''
as $$
declare
    v_uid   uuid := auth.uid();
    v_id    uuid;
    t       jsonb;
    v_tid   uuid;
    v_pos   int := 0;
    v_ids   uuid[] := '{}';
    v_tipo  text;
    v_ops   text[];
    v_cor   int;
begin
    if v_uid is null then
        raise exception 'Debes iniciar sesión';
    end if;
    if jsonb_typeof(p_tarjetas) is distinct from 'array' or jsonb_array_length(p_tarjetas) = 0 then
        raise exception 'El mazo necesita al menos una tarjeta';
    end if;

    if p_mazo_id is null then
        insert into public.mazos (creador_id, nombre, descripcion, categoria_id, color, es_publico)
        values (v_uid, btrim(p_mazo ->> 'nombre'), nullif(btrim(p_mazo ->> 'descripcion'), ''),
                coalesce(p_mazo ->> 'categoria_id', 'otros'), coalesce(p_mazo ->> 'color', 'ROSA'),
                coalesce((p_mazo ->> 'es_publico')::boolean, false))
        returning id into v_id;
    else
        update public.mazos
        set nombre = btrim(p_mazo ->> 'nombre'),
            descripcion = nullif(btrim(p_mazo ->> 'descripcion'), ''),
            categoria_id = coalesce(p_mazo ->> 'categoria_id', categoria_id),
            color = coalesce(p_mazo ->> 'color', color),
            es_publico = coalesce((p_mazo ->> 'es_publico')::boolean, es_publico),
            updated_at = now()
        where id = p_mazo_id and creador_id = v_uid
        returning id into v_id;
        if v_id is null then
            raise exception 'El mazo no existe o no es tuyo';
        end if;
    end if;

    for t in select value from jsonb_array_elements(p_tarjetas) loop
        v_pos := v_pos + 1;
        v_tipo := t ->> 'tipo';
        v_tid := nullif(t ->> 'id', '')::uuid;
        if v_tid is not null and not exists (select 1 from public.tarjetas where id = v_tid and mazo_id = v_id) then
            raise exception 'La tarjeta % no pertenece a este mazo', v_pos;
        end if;

        if v_tipo = 'opcion_multiple' then
            v_ops := array(select btrim(o) from jsonb_array_elements_text(t -> 'opciones') o);
            v_cor := (t ->> 'correcta')::int;
            if coalesce(btrim(t ->> 'enunciado'), '') = '' then
                raise exception 'La pregunta % no tiene enunciado', v_pos;
            end if;
            if cardinality(v_ops) < 2 or exists (select 1 from unnest(v_ops) o where o = '')
               or (select count(distinct lower(o)) from unnest(v_ops) o) <> cardinality(v_ops) then
                raise exception 'La pregunta % necesita al menos 2 opciones distintas y no vacías', v_pos;
            end if;
            if v_cor is null or v_cor < 0 or v_cor >= cardinality(v_ops) then
                raise exception 'Marca la respuesta correcta de la pregunta %', v_pos;
            end if;

            if v_tid is null then
                insert into public.tarjetas (mazo_id, tipo, posicion, enunciado, opciones)
                values (v_id, v_tipo, v_pos, btrim(t ->> 'enunciado'), v_ops)
                returning id into v_tid;
            else
                update public.tarjetas
                set tipo = v_tipo, posicion = v_pos, enunciado = btrim(t ->> 'enunciado'), opciones = v_ops,
                    termino = null, definicion = null, updated_at = now()
                where id = v_tid;
            end if;
            insert into public.claves (tarjeta_id, indice_correcto, explicacion)
            values (v_tid, v_cor, nullif(btrim(t ->> 'explicacion'), ''))
            on conflict (tarjeta_id) do update
                set indice_correcto = excluded.indice_correcto, explicacion = excluded.explicacion;

        elsif v_tipo = 'definicion' then
            if coalesce(btrim(t ->> 'termino'), '') = '' or coalesce(btrim(t ->> 'definicion'), '') = '' then
                raise exception 'La tarjeta % necesita término y definición', v_pos;
            end if;
            if v_tid is null then
                insert into public.tarjetas (mazo_id, tipo, posicion, termino, definicion)
                values (v_id, v_tipo, v_pos, btrim(t ->> 'termino'), btrim(t ->> 'definicion'))
                returning id into v_tid;
            else
                update public.tarjetas
                set tipo = v_tipo, posicion = v_pos, termino = btrim(t ->> 'termino'),
                    definicion = btrim(t ->> 'definicion'), enunciado = null, opciones = null, updated_at = now()
                where id = v_tid;
                delete from public.claves where tarjeta_id = v_tid;
            end if;
        else
            raise exception 'La tarjeta % tiene un tipo no válido', v_pos;
        end if;

        v_ids := v_ids || v_tid;
    end loop;

    -- Las tarjetas que el usuario quitó del mazo
    delete from public.tarjetas where mazo_id = v_id and not (id = any (v_ids));
    return v_id;
end;
$$;

revoke execute on function public.guardar_mazo(uuid, jsonb, jsonb) from public, anon;
grant execute on function public.guardar_mazo(uuid, jsonb, jsonb) to authenticated;

-- Para editar un mazo propio el editor necesita saber qué opción es la correcta y su explicación.
-- Solo responde al creador del mazo; a cualquier otro usuario le devuelve una lista vacía.
create function public.claves_de_mi_mazo(p_mazo_id uuid)
returns table (tarjeta_id uuid, indice_correcto int, explicacion text)
language sql
stable
security definer
set search_path = ''
as $$
    select c.tarjeta_id, c.indice_correcto, c.explicacion
    from public.claves c
    join public.tarjetas t on t.id = c.tarjeta_id
    join public.mazos m on m.id = t.mazo_id
    where t.mazo_id = p_mazo_id and m.creador_id = auth.uid();
$$;

revoke execute on function public.claves_de_mi_mazo(uuid) from public, anon;
grant execute on function public.claves_de_mi_mazo(uuid) to authenticated;


-- =====================================================================
-- 8. ESTUDIO (v2): sesiones con 4 modos, repetición espaciada y XP semanal
-- =====================================================================
-- Se retira el esquema de partidas de la versión 1 (preguntas con respuesta única).
do $$
declare f record;
begin
    for f in select oid::regprocedure as firma from pg_proc
             where pronamespace = 'public'::regnamespace and proname in ('crear_mazo', 'iniciar_partida', 'responder')
    loop
        execute 'drop function ' || f.firma;
    end loop;
end $$;
drop table if exists public.respuestas_partida, public.resultados, public.respuestas_correctas, public.preguntas cascade;

-- "Hoy" y "esta semana" se cuentan en la hora de Colombia, no en UTC.
create or replace function public.hoy_local() returns date
language sql stable set search_path = '' as $$ select (now() at time zone 'America/Bogota')::date $$;

create or replace function public.inicio_semana(p_dia date) returns date
language sql immutable set search_path = '' as $$ select p_dia - (extract(isodow from p_dia)::int - 1) $$;

-- Una sesión de estudio. Solo se crea y modifica con las funciones de abajo:
-- la app puede leer sus propias sesiones pero no escribirlas.
create table public.sesiones (
    id              uuid primary key default gen_random_uuid(),
    usuario_id      uuid not null references public.perfiles (id) on delete cascade,
    mazo_id         uuid references public.mazos (id) on delete set null,
    mazo_nombre     text not null,               -- copia: el historial sobrevive si se borra el mazo
    mazo_color      text not null default 'ROSA',
    modo            text not null check (modo in ('practica', 'contrarreloj', 'repaso', 'reto')),
    estado          text not null default 'en_curso'
                    check (estado in ('en_curso', 'completada', 'fallida', 'tiempo_agotado', 'abandonada')),
    tarjetas        uuid[] not null,             -- orden en que se presentan
    vidas           int,                         -- solo reto (empieza en 3)
    limite_segundos int,                         -- solo contrarreloj
    aciertos        int not null default 0,      -- preguntas acertadas
    errores         int not null default 0,      -- preguntas falladas
    lo_se           int not null default 0,      -- definiciones marcadas «Lo sé»
    practicar       int not null default 0,      -- definiciones marcadas «Necesito practicar»
    combo           int not null default 0,
    mejor_combo     int not null default 0,
    xp              int not null default 0,      -- XP sumado respuesta a respuesta
    xp_bono         int not null default 0,      -- bonos al terminar (sesión perfecta, tiempo, XP doble)
    xp_total        int not null default 0,      -- lo que realmente se acreditó al perfil
    xp_doble        boolean not null default false,
    iniciada_en     timestamptz not null default now(),
    terminada_en    timestamptz
);
create index sesiones_usuario_idx on public.sesiones (usuario_id, iniciada_en desc);
create index sesiones_mazo_id_idx on public.sesiones (mazo_id);

alter table public.sesiones enable row level security;
revoke all on public.sesiones from anon, authenticated;
grant select on public.sesiones to authenticated;
create policy "Ver mis sesiones" on public.sesiones for select to authenticated
    using (usuario_id = (select auth.uid()));

-- Cada tarjeta respondida dentro de una sesión (sin acceso directo desde la app).
create table public.respuestas_sesion (
    id             bigint generated always as identity primary key,
    sesion_id      uuid not null references public.sesiones (id) on delete cascade,
    tarjeta_id     uuid references public.tarjetas (id) on delete set null,
    orden          int not null,
    resultado      text not null check (resultado in ('correcta', 'incorrecta', 'lo_se', 'practicar')),
    indice_elegido int,
    xp             int not null default 0,
    respondida_en  timestamptz not null default now(),
    unique (sesion_id, tarjeta_id)
);
create index respuestas_sesion_tarjeta_idx on public.respuestas_sesion (tarjeta_id);
alter table public.respuestas_sesion enable row level security;
revoke all on public.respuestas_sesion from anon, authenticated;

-- Repetición espaciada (sistema de cajas de Leitner): caja 0 = repasar hoy … caja 5 = dominada.
-- Intervalos por caja: 0, 1, 3, 7, 14 y 30 días.
create table public.progreso_tarjetas (
    usuario_id  uuid not null references public.perfiles (id) on delete cascade,
    tarjeta_id  uuid not null references public.tarjetas (id) on delete cascade,
    caja        int not null default 0 check (caja between 0 and 5),
    aciertos    int not null default 0,
    fallos      int not null default 0,
    proxima     date not null,
    ultima      timestamptz not null default now(),
    primary key (usuario_id, tarjeta_id)
);
create index progreso_tarjetas_tarjeta_idx on public.progreso_tarjetas (tarjeta_id);
alter table public.progreso_tarjetas enable row level security;
revoke all on public.progreso_tarjetas from anon, authenticated;
grant select on public.progreso_tarjetas to authenticated;
create policy "Ver mi progreso" on public.progreso_tarjetas for select to authenticated
    using (usuario_id = (select auth.uid()));

-- XP ganado por semana (lunes a domingo). Es público para el ranking y se emite por Realtime.
create table public.xp_semanal (
    usuario_id uuid not null references public.perfiles_publicos (id) on delete cascade,
    semana     date not null,
    xp         int not null default 0,
    primary key (usuario_id, semana)
);
create index xp_semanal_semana_idx on public.xp_semanal (semana, xp desc);
alter table public.xp_semanal enable row level security;
revoke all on public.xp_semanal from anon, authenticated;
grant select on public.xp_semanal to authenticated;
create policy "Ranking visible para usuarios" on public.xp_semanal for select to authenticated using (true);
alter publication supabase_realtime add table public.xp_semanal;

-- ¿El usuario ya completó un reto diario hoy? (el XP doble es solo para el primero del día)
create or replace function public._reto_hecho_hoy(p_usuario uuid) returns boolean
language sql stable set search_path = '' as $$
    select exists (
        select 1 from public.sesiones o
        where o.usuario_id = p_usuario and o.modo = 'reto' and o.estado = 'completada'
          and (o.terminada_en at time zone 'America/Bogota')::date = public.hoy_local())
$$;

-- Resumen de una sesión (pantalla 12 y detalle del historial). Uso interno.
create or replace function public._resumen_sesion(p_id uuid) returns jsonb
language sql stable security definer set search_path = '' as $$
    select to_jsonb(s) - 'tarjetas' - 'usuario_id'
        || jsonb_build_object(
            'total_tarjetas', cardinality(s.tarjetas),
            'revisadas', (select count(*) from public.respuestas_sesion r where r.sesion_id = s.id),
            'refuerzo', coalesce((
                select jsonb_agg(coalesce(t.enunciado, t.termino) order by r.orden)
                from public.respuestas_sesion r join public.tarjetas t on t.id = r.tarjeta_id
                where r.sesion_id = s.id and r.resultado in ('incorrecta', 'practicar')), '[]'::jsonb))
    from public.sesiones s where s.id = p_id
$$;

-- Cierra una sesión, calcula los bonos y acredita el XP al perfil y al ranking semanal. Uso interno.
create or replace function public._cerrar_sesion(p_id uuid, p_estado text) returns jsonb
language plpgsql security definer set search_path = '' as $$
declare
    s       public.sesiones%rowtype;
    v_bono  int := 0;
    v_doble boolean := false;
    v_total int := 0;
begin
    select * into s from public.sesiones where id = p_id for update;
    if s.estado <> 'en_curso' then return public._resumen_sesion(p_id); end if;

    if p_estado <> 'abandonada' then
        -- Sesión perfecta: al menos 3 preguntas y ningún error
        if s.aciertos >= 3 and s.errores = 0 then v_bono := v_bono + 10; end if;
        -- Contrarreloj terminado antes de tiempo: 1 XP por cada 5 segundos sobrantes
        if s.modo = 'contrarreloj' and p_estado = 'completada' then
            v_bono := v_bono + greatest(0, floor((s.limite_segundos - extract(epoch from now() - s.iniciada_en)) / 5))::int;
        end if;
        -- Reto diario: XP doble solo en el primer reto completado del día
        if s.modo = 'reto' and p_estado = 'completada' and not public._reto_hecho_hoy(s.usuario_id) then
            v_doble := true;
            v_bono := v_bono + s.xp + v_bono;
        end if;
        v_total := s.xp + v_bono;
    end if;   -- al abandonar se pierde el XP de la sesión

    update public.sesiones
       set estado = p_estado, terminada_en = now(), combo = 0,
           xp_bono = v_bono, xp_total = v_total, xp_doble = v_doble
     where id = p_id;

    if v_total > 0 then
        update public.perfiles set xp = xp + v_total where id = s.usuario_id;
        insert into public.xp_semanal (usuario_id, semana, xp)
        values (s.usuario_id, public.inicio_semana(public.hoy_local()), v_total)
        on conflict (usuario_id, semana) do update set xp = public.xp_semanal.xp + excluded.xp;
    end if;
    return public._resumen_sesion(p_id);
end $$;

-- Inicia una sesión: elige las tarjetas según el modo y devuelve las tarjetas sin las respuestas.
create or replace function public.iniciar_sesion(p_mazo_id uuid, p_modo text) returns jsonb
language plpgsql security definer set search_path = '' as $$
declare
    v_uid    uuid := auth.uid();
    v_mazo   public.mazos%rowtype;
    v_ids    uuid[];
    v_id     uuid;
    v_limite int;
    v_vidas  int;
    v_doble  boolean := false;
begin
    if v_uid is null then raise exception 'Debes iniciar sesión'; end if;
    if p_modo is null or p_modo not in ('practica', 'contrarreloj', 'repaso', 'reto') then
        raise exception 'Modo de juego no válido';
    end if;
    select * into v_mazo from public.mazos m where m.id = p_mazo_id and (m.creador_id = v_uid or m.es_publico);
    if not found then raise exception 'El mazo no existe o ya no es público'; end if;

    -- Una sola sesión en curso por usuario: la anterior queda abandonada
    perform public._cerrar_sesion(o.id, 'abandonada')
       from public.sesiones o where o.usuario_id = v_uid and o.estado = 'en_curso';

    if p_modo = 'repaso' then
        -- Pendientes de hoy: primero las falladas o marcadas «Necesito practicar» (caja baja), luego las nuevas
        select array_agg(x.id order by x.n) into v_ids from (
            select t.id, row_number() over (
                       order by coalesce(p.caja, 0), (p.tarjeta_id is null), p.fallos desc nulls last, random()) as n
              from public.tarjetas t
              left join public.progreso_tarjetas p on p.tarjeta_id = t.id and p.usuario_id = v_uid
             where t.mazo_id = p_mazo_id and (p.proxima is null or p.proxima <= public.hoy_local())
             order by n limit 20) x;
    else
        select array_agg(x.id order by x.n) into v_ids from (
            select t.id, row_number() over (order by random()) as n
              from public.tarjetas t where t.mazo_id = p_mazo_id
             order by n limit case p_modo when 'reto' then 10 else 50 end) x;
    end if;

    if v_ids is null then
        if p_modo = 'repaso' then raise exception 'No tienes tarjetas pendientes de repaso en este mazo'; end if;
        raise exception 'Este mazo todavía no tiene tarjetas';
    end if;

    if p_modo = 'contrarreloj' then v_limite := least(300, greatest(45, cardinality(v_ids) * 12)); end if;
    if p_modo = 'reto' then
        v_vidas := 3;
        v_doble := not public._reto_hecho_hoy(v_uid);
    end if;

    insert into public.sesiones (usuario_id, mazo_id, mazo_nombre, mazo_color, modo, tarjetas, vidas, limite_segundos)
    values (v_uid, p_mazo_id, v_mazo.nombre, v_mazo.color, p_modo, v_ids, v_vidas, v_limite)
    returning id into v_id;

    return jsonb_build_object(
        'sesion_id', v_id, 'modo', p_modo, 'mazo_nombre', v_mazo.nombre, 'mazo_color', v_mazo.color,
        'vidas', v_vidas, 'limite_segundos', v_limite, 'xp_doble_disponible', v_doble,
        'tarjetas', (
            select jsonb_agg(jsonb_build_object(
                       'id', t.id, 'tipo', t.tipo, 'enunciado', t.enunciado, 'opciones', t.opciones,
                       'termino', t.termino, 'definicion', t.definicion, 'caja', p.caja) order by o.n)
              from unnest(v_ids) with ordinality as o(id, n)
              join public.tarjetas t on t.id = o.id
              left join public.progreso_tarjetas p on p.tarjeta_id = t.id and p.usuario_id = v_uid));
end $$;

-- Registra la respuesta a una tarjeta. El servidor valida la opción, calcula XP, combo y vidas,
-- actualiza la repetición espaciada y, si era la última tarjeta (o se acabaron las vidas), cierra la sesión.
create or replace function public.responder_tarjeta(
    p_sesion_id uuid, p_tarjeta_id uuid, p_opcion int default null, p_lo_se boolean default null
) returns jsonb
language plpgsql security definer set search_path = '' as $$
declare
    s       public.sesiones%rowtype;
    t       public.tarjetas%rowtype;
    c       public.claves%rowtype;
    v_res   text;
    v_bien  boolean;
    v_xp    int := 0;
    v_caja  int;
    v_orden int;
    v_fin   text;
begin
    select * into s from public.sesiones where id = p_sesion_id and usuario_id = auth.uid() for update;
    if not found then raise exception 'La sesión no existe'; end if;
    if s.estado <> 'en_curso' then raise exception 'Esta sesión ya terminó'; end if;
    if not (p_tarjeta_id = any (s.tarjetas)) then raise exception 'La tarjeta no pertenece a esta sesión'; end if;
    if exists (select 1 from public.respuestas_sesion r where r.sesion_id = s.id and r.tarjeta_id = p_tarjeta_id) then
        raise exception 'Esa tarjeta ya fue respondida';
    end if;

    -- Contrarreloj: pasada la hora límite (con 3 s de gracia por la red) la respuesta ya no cuenta
    if s.modo = 'contrarreloj' and now() > s.iniciada_en + make_interval(secs => s.limite_segundos + 3) then
        return jsonb_build_object('tiempo_agotado', true, 'resumen', public._cerrar_sesion(s.id, 'tiempo_agotado'));
    end if;

    select * into t from public.tarjetas where id = p_tarjeta_id;
    if not found then raise exception 'La tarjeta ya no existe'; end if;

    if t.tipo = 'opcion_multiple' then
        if p_opcion is null then raise exception 'Elige una opción'; end if;
        select * into c from public.claves where tarjeta_id = t.id;
        v_bien := p_opcion = c.indice_correcto;
        if v_bien then
            s.aciertos := s.aciertos + 1;
            s.combo := s.combo + 1;
            -- 10 XP por acierto; en Contrarreloj el combo suma hasta 10 XP más
            v_xp := 10 + case when s.modo = 'contrarreloj' then 2 * least(s.combo - 1, 5) else 0 end;
            v_res := 'correcta';
        else
            s.errores := s.errores + 1;
            s.combo := 0;
            if s.modo = 'reto' then s.vidas := s.vidas - 1; end if;
            v_res := 'incorrecta';
        end if;
    else
        if p_lo_se is null then raise exception 'Marca «Lo sé» o «Necesito practicar»'; end if;
        -- Las definiciones nunca quitan vidas: ambas respuestas dan 2 XP para no premiar mentir
        v_bien := p_lo_se;
        v_xp := 2;
        if p_lo_se then s.lo_se := s.lo_se + 1; v_res := 'lo_se';
        else s.practicar := s.practicar + 1; v_res := 'practicar'; end if;
        -- En Contrarreloj la racha también cuenta los conceptos dominados
        if s.modo = 'contrarreloj' then
            if p_lo_se then s.combo := s.combo + 1; v_xp := v_xp + least(s.combo - 1, 5);
            else s.combo := 0; end if;
        end if;
    end if;
    s.mejor_combo := greatest(s.mejor_combo, s.combo);
    s.xp := s.xp + v_xp;

    update public.sesiones
       set aciertos = s.aciertos, errores = s.errores, lo_se = s.lo_se, practicar = s.practicar,
           combo = s.combo, mejor_combo = s.mejor_combo, vidas = s.vidas, xp = s.xp
     where id = s.id;

    select count(*) + 1 into v_orden from public.respuestas_sesion where sesion_id = s.id;
    insert into public.respuestas_sesion (sesion_id, tarjeta_id, orden, resultado, indice_elegido, xp)
    values (s.id, t.id, v_orden, v_res, p_opcion, v_xp);

    -- Repetición espaciada: acierto o «Lo sé» sube una caja; fallo o «Necesito practicar» vuelve a la 0
    insert into public.progreso_tarjetas as p (usuario_id, tarjeta_id, caja, aciertos, fallos, proxima)
    values (s.usuario_id, t.id, case when v_bien then 1 else 0 end, v_bien::int, (not v_bien)::int,
            public.hoy_local() + case when v_bien then 1 else 0 end)
    on conflict (usuario_id, tarjeta_id) do update set
        caja     = case when v_bien then least(p.caja + 1, 5) else 0 end,
        aciertos = p.aciertos + v_bien::int,
        fallos   = p.fallos + (not v_bien)::int,
        proxima  = public.hoy_local()
                   + (array[0, 1, 3, 7, 14, 30])[(case when v_bien then least(p.caja + 1, 5) else 0 end) + 1],
        ultima   = now()
    returning caja into v_caja;

    if s.modo = 'reto' and s.vidas <= 0 then v_fin := 'fallida';
    elsif v_orden >= cardinality(s.tarjetas) then v_fin := 'completada';
    end if;

    return jsonb_build_object(
        'resultado', v_res,
        'correcta', case when t.tipo = 'opcion_multiple' then v_bien end,
        'indice_correcto', c.indice_correcto, 'explicacion', c.explicacion,
        'xp', v_xp, 'xp_sesion', s.xp, 'combo', s.combo, 'vidas', s.vidas, 'caja', v_caja,
        'resumen', case when v_fin is not null then public._cerrar_sesion(s.id, v_fin) end);
end $$;

-- Salir de la sesión. En Contrarreloj, si el tiempo ya se acabó, termina por tiempo (y conserva el XP);
-- en otro caso se abandona y se pierde el XP. Si ya estaba cerrada, devuelve su resumen.
create or replace function public.terminar_sesion(p_sesion_id uuid) returns jsonb
language plpgsql security definer set search_path = '' as $$
declare s public.sesiones%rowtype;
begin
    select * into s from public.sesiones where id = p_sesion_id and usuario_id = auth.uid();
    if not found then raise exception 'La sesión no existe'; end if;
    if s.estado <> 'en_curso' then return public._resumen_sesion(s.id); end if;
    if s.modo = 'contrarreloj' and now() >= s.iniciada_en + make_interval(secs => s.limite_segundos - 2) then
        return public._cerrar_sesion(s.id, 'tiempo_agotado');
    end if;
    return public._cerrar_sesion(s.id, 'abandonada');
end $$;

-- Resumen de una de mis sesiones (detalle del historial).
create or replace function public.resumen_sesion(p_sesion_id uuid) returns jsonb
language plpgsql stable security definer set search_path = '' as $$
begin
    if not exists (select 1 from public.sesiones where id = p_sesion_id and usuario_id = auth.uid()) then
        raise exception 'La sesión no existe';
    end if;
    return public._resumen_sesion(p_sesion_id);
end $$;

-- Datos de la pantalla de selección de modo (pantalla 10).
create or replace function public.info_modos(p_mazo_id uuid) returns jsonb
language sql stable security definer set search_path = '' as $$
    select jsonb_build_object(
        'total', count(t.id),
        'preguntas', count(t.id) filter (where t.tipo = 'opcion_multiple'),
        'definiciones', count(t.id) filter (where t.tipo = 'definicion'),
        'pendientes_repaso', count(t.id) filter (where p.proxima is null or p.proxima <= public.hoy_local()),
        'dominadas', count(t.id) filter (where p.caja >= 4),
        'xp_doble_disponible', not public._reto_hecho_hoy(auth.uid()))
    from public.mazos m
    join public.tarjetas t on t.mazo_id = m.id
    left join public.progreso_tarjetas p on p.tarjeta_id = t.id and p.usuario_id = auth.uid()
    where m.id = p_mazo_id and (m.creador_id = auth.uid() or m.es_publico)
$$;

-- Datos del panel de Inicio: XP, racha de días, actividad, repasos pendientes y último mazo estudiado.
create or replace function public.resumen_inicio() returns jsonb
language sql stable security definer set search_path = '' as $$
    with mias as (
        select * from public.sesiones
        where usuario_id = auth.uid() and estado in ('completada', 'fallida', 'tiempo_agotado')
    ), dias as (
        select distinct (terminada_en at time zone 'America/Bogota')::date as d from mias
    ), grupos as (
        select d, d - (row_number() over (order by d))::int as g from dias
    ), ultimo as (
        select s.mazo_id, s.mazo_nombre, s.mazo_color, s.modo, s.terminada_en
          from mias s join public.mazos m on m.id = s.mazo_id and (m.creador_id = auth.uid() or m.es_publico)
         order by s.terminada_en desc limit 1
    )
    select jsonb_build_object(
        'xp', (select xp from public.perfiles where id = auth.uid()),
        'xp_semana', coalesce((select xp from public.xp_semanal
                               where usuario_id = auth.uid() and semana = public.inicio_semana(public.hoy_local())), 0),
        'racha', case when (select max(d) from dias) >= public.hoy_local() - 1
                      then (select count(*) from grupos where g = (select g from grupos order by d desc limit 1))
                      else 0 end,
        'estudio_hoy', exists (select 1 from dias where d = public.hoy_local()),
        'sesiones', (select count(*) from mias),
        'tarjetas_estudiadas', (select count(*) from public.respuestas_sesion r join mias s on s.id = r.sesion_id),
        'reto_hoy', public._reto_hecho_hoy(auth.uid()),
        'pendientes_repaso', (
            select count(*) from public.progreso_tarjetas p
              join public.tarjetas t on t.id = p.tarjeta_id
              join public.mazos m on m.id = t.mazo_id
             where p.usuario_id = auth.uid() and p.proxima <= public.hoy_local()
               and (m.creador_id = auth.uid() or m.es_publico)),
        'continuar', (select to_jsonb(u) from ultimo u))
$$;

-- Permisos: las funciones internas no se exponen; las públicas solo para usuarios con sesión.
revoke execute on function public._reto_hecho_hoy(uuid), public._resumen_sesion(uuid),
    public._cerrar_sesion(uuid, text) from public, anon, authenticated;
revoke execute on function public.iniciar_sesion(uuid, text), public.responder_tarjeta(uuid, uuid, int, boolean),
    public.terminar_sesion(uuid), public.resumen_sesion(uuid), public.info_modos(uuid), public.resumen_inicio()
    from public, anon;
grant execute on function public.iniciar_sesion(uuid, text), public.responder_tarjeta(uuid, uuid, int, boolean),
    public.terminar_sesion(uuid), public.resumen_sesion(uuid), public.info_modos(uuid), public.resumen_inicio()
    to authenticated;
