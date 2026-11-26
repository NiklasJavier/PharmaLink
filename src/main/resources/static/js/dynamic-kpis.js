/**
 * Renders dynamic KPI cards from a JSON string into a specified root element.
 *
 * @param {string} jsonString The JSON string containing an object with key-value pairs.
 * @param {string} rootElementId The ID of the HTML element where the cards should be rendered.
 * @param {string} title A title for debugging purposes (optional).
 */
function renderDynamicKPIsFromJson(jsonString, rootElementId, title) {
    const rootElement = document.getElementById(rootElementId);
    if (!rootElement) {
        console.error(`KPI Renderer: Root element with ID '${rootElementId}' not found.`);
        return;
    }

    try {
        const data = JSON.parse(jsonString);

        // Create the container for the KPI cards
        const kpiGrid = document.createElement('div');
        kpiGrid.className = 'kpi-grid';

        // Iterate over the key-value pairs in the JSON object
        for (const key in data) {
            if (Object.hasOwnProperty.call(data, key)) {
                const value = data[key];

                // Create a single KPI card
                const card = document.createElement('div');
                card.className = 'kpi-card';

                // The value (the large number)
                const valueElement = document.createElement('div');
                valueElement.className = 'kpi-value';
                valueElement.textContent = value;
                card.appendChild(valueElement);

                // The key (the label)
                const keyElement = document.createElement('div');
                keyElement.className = 'kpi-key';
                keyElement.textContent = key;
                card.appendChild(keyElement);

                kpiGrid.appendChild(card);
            }
        }

        // Clear the root element and append the new grid
        rootElement.innerHTML = '';
        rootElement.appendChild(kpiGrid);

    } catch (error) {
        console.error(`Error parsing or rendering KPIs for '${title}':`, error);
        rootElement.innerHTML = '<p class="text-danger">Error loading KPIs.</p>';
    }
}