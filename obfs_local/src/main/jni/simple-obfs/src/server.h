/*
 * server.h - Define server's buffers and callbacks
 *
 * Copyright (C) 2013 - 2016, Max Lv <max.c.lv@gmail.com>
 *
 * This file is part of the simple-obfs.
 *
 * simple-obfs is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * simple-obfs is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with simple-obfs; see the file COPYING. If not, see
 * <http://www.gnu.org/licenses/>.
 */

#ifndef _SERVER_H
#define _SERVER_H

#include <ev.h>
#include <time.h>
#include <libcork/ds.h>

#include "encrypt.h"
#include "jconf.h"

#include "common.h"

typedef struct listen_ctx {
    ev_io io;
    int fd;
    int timeout;
    int method;
    ss_addr_t *dst_addr;
    ss_addr_t *failover;
    char *iface;
    struct ev_loop *loop;
} listen_ctx_t;

typedef struct server_ctx {
    ev_io io;
    ev_timer watcher;
    int connected;
    struct server *server;
} server_ctx_t;

typedef struct server {
    int fd;
    int stage;
    int auth;

    obfs_t *obfs;

    buffer_t *buf;
    buffer_t *header_buf;

    struct server_ctx *recv_ctx;
    struct server_ctx *send_ctx;
    struct listen_ctx *listen_ctx;
    struct remote *remote;

    struct cork_dllist_item entries;
} server_t;

typedef struct remote_ctx {
    ev_io io;
    int connected;
    struct remote *remote;
} remote_ctx_t;

typedef struct remote {
    int fd;
    buffer_t *buf;
    struct remote_ctx *recv_ctx;
    struct remote_ctx *send_ctx;
    struct server *server;
} remote_t;

#endif // _SERVER_H
