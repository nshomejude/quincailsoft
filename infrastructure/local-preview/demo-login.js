const previewLogo = new URL('../logo.png', document.currentScript.src);

document.addEventListener('DOMContentLoaded', () => {
    if (location.origin !== 'http://localhost:8180' ||
        !location.pathname.startsWith('/realms/quincaisoft/')) return;

    document.body.classList.add('quincai-login');
    const stylesheet = document.createElement('link');
    stylesheet.rel = 'stylesheet';
    stylesheet.href = new URL('login.css', previewLogo).href;
    document.head.append(stylesheet);
    const header = document.getElementById('kc-header-wrapper');
    if (header) {
        const logo = document.createElement('img');
        logo.src = previewLogo.href;
        logo.alt = 'QuincaiSoft';
        const logoSurface = document.createElement('div');
        logoSurface.className = 'quincai-logo-surface';
        logoSurface.append(logo);
        const copy = document.createElement('div');
        copy.className = 'quincai-copy';
        const eyebrow = document.createElement('p');
        eyebrow.textContent = 'ONE BUSINESS. ONE WORKSPACE.';
        const message = document.createElement('strong');
        message.textContent = 'Everything connected. Every role in control.';
        const description = document.createElement('p');
        description.textContent = 'Sales, stock, purchasing and finance come together in a single, consistent workspace.';
        copy.append(eyebrow, message, description);
        header.replaceChildren(logoSurface, copy);
    }

    const form = document.getElementById('kc-form-login');
    const username = document.getElementById('username');
    const password = document.getElementById('password');
    const restart = document.getElementById('reset-login');
    if (!form || !password || (!username && !restart) || new URL(form.action).origin !== location.origin) return;

    const pendingKey = 'quincai-preview-role';
    const signIn = (role) => {
        if (!username) {
            sessionStorage.setItem(pendingKey, role);
            restart.click();
            return;
        }
        username.value = 'preview-' + role;
        password.value = previewAccounts[role];
        const submit = form.querySelector('button[type="submit"], input[type="submit"]');
        form.requestSubmit(submit ?? undefined);
    };
    const previous = sessionStorage.getItem(pendingKey);
    if (previous) {
        sessionStorage.removeItem(pendingKey);
        if (username && Object.hasOwn(previewAccounts, previous)) {
            signIn(previous);
            return;
        }
    }

    const options = document.createElement('fieldset');
    options.style.cssText = 'display:flex;gap:.5rem;flex-wrap:wrap;margin-bottom:1rem';
    const label = document.createElement('legend');
    label.textContent = 'Local demo sign-in';
    options.append(label);
    for (const role of Object.keys(previewAccounts)) {
        const button = document.createElement('button');
        button.type = 'button';
        button.className = 'pf-v5-c-button pf-m-secondary';
        button.textContent = 'Continue as ' + role;
        button.addEventListener('click', () => signIn(role));
        options.append(button);
    }
    form.prepend(options);
});
