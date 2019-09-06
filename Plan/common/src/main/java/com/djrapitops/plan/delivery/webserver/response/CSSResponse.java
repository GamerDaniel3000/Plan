/*
 *  This file is part of Player Analytics (Plan).
 *
 *  Plan is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser General Public License v3 as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Plan is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with Plan. If not, see <https://www.gnu.org/licenses/>.
 */
package com.djrapitops.plan.delivery.webserver.response;

import com.djrapitops.plan.settings.locale.Locale;
import com.djrapitops.plan.settings.theme.Theme;
import com.djrapitops.plan.storage.file.PlanFiles;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;

/**
 * @author Rsl1122
 */
public class CSSResponse extends FileResponse {

    public CSSResponse(String fileName, PlanFiles files) throws IOException {
        super(format(fileName), files);
        super.setType(ResponseType.CSS);
        setContent(getContent());
    }

    @Override
    public void send(HttpExchange exchange, Locale locale, Theme theme) throws IOException {
        fixThemeColors(theme);
        super.send(exchange, locale, theme);
    }
}
