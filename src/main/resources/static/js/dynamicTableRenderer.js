// src/main/resources/static/js/dynamicTableRenderer.js

/**
 * Dynamically renders an HTML table from a JSON array of objects.
 * Column names are derived from the keys of the first object.
 * The table becomes scrollable when it exceeds a specified maximum height.
 *
 * @param {string} jsonString The JSON string representing an array of objects.
 * @param {string} targetElementId The ID of the HTML element where the table should be rendered.
 * @param {number} maxBodyHeight The maximum height (in pixels) of the table body before scrolling.
 * @param {string} tableTitle Optional title for the table (not rendered here, as the fragment handles it).
 */
function renderDynamicTableFromJson(jsonString, targetElementId, maxBodyHeight, tableTitle = '') {
    const targetElement = document.getElementById(targetElementId);

    if (!targetElement) {
        console.error(`Error: Target element with ID '${targetElementId}' not found.`);
        return;
    }

    let data;
    try {
        data = JSON.parse(jsonString);
    } catch (e) {
        console.error(`Error: Invalid JSON format for element with ID '${targetElementId}'.`, e);
        targetElement.innerHTML = `<p style="color: red;">Error: Invalid JSON format for data.</p>`;
        return;
    }

    if (!Array.isArray(data) || data.length === 0) {
        targetElement.innerHTML = `<p>No data available.</p>`;
        return;
    }

    const headers = (typeof data[0] === 'object' && data[0] !== null) ? Object.keys(data[0]) : [];

    if (headers.length === 0) {
        targetElement.innerHTML = `<p>No columns found for the data.</p>`;
        return;
    }

    const table = document.createElement('table');
    table.className = 'dynamic-generated-table';

    const thead = table.createTHead();
    const headerRow = thead.insertRow();
    headers.forEach(headerText => {
        const th = document.createElement('th');
        th.textContent = headerText;
        headerRow.appendChild(th);
    });

    const tbody = table.createTBody();
    data.forEach(rowObject => {
        const row = tbody.insertRow();
        headers.forEach(headerKey => {
            const cell = row.insertCell();
            let cellValue = rowObject[headerKey];

            const MAX_CHARS_DISPLAY = 30;
            const TRUNCATE_SUFFIX = '...';

            // NEW: Styling for the 'successful' column
            if (headerKey === 'successful') {
                if (typeof cellValue === 'boolean') {
                    if (cellValue === true) {
                        cell.classList.add('cell-successful-true');
                        cell.textContent = 'True'; // Standardize display to 'True'/'False'
                    } else {
                        cell.classList.add('cell-successful-false');
                        cell.textContent = 'False';
                    }
                } else {
                    // If the value is not a boolean, simply display it as a string
                    cell.textContent = String(cellValue);
                }
            } else if (typeof cellValue === 'object' && cellValue !== null) {
                try {
                    cellValue = JSON.stringify(cellValue, null, 2);
                    if (cellValue.length > MAX_CHARS_DISPLAY) {
                        cell.textContent = cellValue.substring(0, MAX_CHARS_DISPLAY) + TRUNCATE_SUFFIX;
                        cell.title = cellValue;
                        cell.classList.add('truncated-cell');
                    } else {
                        cell.textContent = cellValue;
                    }
                    cell.classList.add('json-cell-content');
                } catch (e) {
                    cell.textContent = '[Invalid object]';
                }
            } else if (cellValue === null || cellValue === undefined || (typeof cellValue === 'string' && cellValue.trim() === '')) {
                cell.textContent = '-';
                cell.classList.add('empty-cell-content');
            } else {
                let stringValue = String(cellValue);
                if (stringValue.length > MAX_CHARS_DISPLAY) {
                    cell.textContent = stringValue.substring(0, MAX_CHARS_DISPLAY) + TRUNCATE_SUFFIX;
                    cell.title = stringValue;
                    cell.classList.add('truncated-cell');
                } else {
                    cell.textContent = stringValue;
                }
            }
        });
    });

    const scrollableBodyContainer = document.createElement('div');
    scrollableBodyContainer.className = 'dynamic-table-scroll-container';
    scrollableBodyContainer.style.maxHeight = `${maxBodyHeight}px`;
    scrollableBodyContainer.style.overflowY = 'auto';

    scrollableBodyContainer.appendChild(table);

    targetElement.innerHTML = '';
    targetElement.appendChild(scrollableBodyContainer);
}