/**
 * Adds global "Copy-on-Click" functionality.
 * Displays a checkmark icon next to the text as confirmation.
 */
document.addEventListener('DOMContentLoaded', () => {

    // SVG markup for the checkmark icon
    const checkmarkSVG = `
        <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" viewBox="0 0 16 16">
            <path d="M13.854 3.646a.5.5 0 0 1 0 .708l-7 7a.5.5 0 0 1-.708 0l-3.5-3.5a.5.5 0 1 1 .708-.708L6.5 10.293l6.646-6.647a.5.5 0 0 1 .708 0z"/>
        </svg>
    `;

    document.body.addEventListener('click', function(event) {
        const copyTarget = event.target.closest('.copy-on-click');
        if (!copyTarget) {
            return;
        }

        // Prevents displaying multiple icons at the same time
        if (copyTarget.dataset.isAnimating) {
            return;
        }

        const textToCopy = copyTarget.innerText;

        navigator.clipboard.writeText(textToCopy).then(() => {
            // Mark element to ignore double-clicks
            copyTarget.dataset.isAnimating = 'true';

            // Create the icon element
            const icon = document.createElement('span');
            icon.className = 'copy-feedback-icon';
            icon.innerHTML = checkmarkSVG;

            // Insert the icon directly after the clicked element
            copyTarget.insertAdjacentElement('afterend', icon);

            // Remove the icon after 1 second
            setTimeout(() => {
                icon.remove();
                delete copyTarget.dataset.isAnimating;
            }, 1000);

        }).catch(err => {
            console.error('Error copying text: ', err);
        });
    });
});