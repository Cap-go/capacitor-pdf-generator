import './style.css';
import { PdfGenerator } from '@capgo/capacitor-pdf-generator';

const plugin = PdfGenerator;

const actions = [
  {
    id: 'html-base64',
    label: 'Generate from HTML (base64)',
    description: 'Render inline HTML content and return the PDF as a base64 string.',
    inputs: [
      {
        name: 'html',
        label: 'HTML',
        type: 'textarea',
        rows: 6,
        value: '<html><body><h1>Hello Capgo</h1><p>Generated with PdfGenerator.</p></body></html>',
      },
      {
        name: 'documentSize',
        label: 'Document size',
        type: 'select',
        value: 'A4',
        options: [
          { value: 'A4', label: 'A4' },
          { value: 'A3', label: 'A3' },
        ],
      },
      {
        name: 'orientation',
        label: 'Orientation',
        type: 'select',
        value: 'portrait',
        options: [
          { value: 'portrait', label: 'Portrait' },
          { value: 'landscape', label: 'Landscape' },
        ],
      },
      {
        name: 'fileName',
        label: 'File name',
        type: 'text',
        value: 'inline.pdf',
      },
    ],
    run: async (values) => {
      const result = await plugin.fromData({
        data: values.html,
        documentSize: values.documentSize,
        orientation: values.orientation,
        type: 'base64',
        fileName: values.fileName || 'inline.pdf',
      });
      return result;
    },
  },
  {
    id: 'html-share',
    label: 'Generate from HTML (share)',
    description: 'Render inline HTML content and open the native share dialog.',
    inputs: [
      {
        name: 'html',
        label: 'HTML',
        type: 'textarea',
        rows: 6,
        value: '<html><body><h2>Share me!</h2><p>Tap share to export the PDF.</p></body></html>',
      },
      {
        name: 'fileName',
        label: 'File name',
        type: 'text',
        value: 'share.pdf',
      },
    ],
    run: async (values) => {
      const result = await plugin.fromData({
        data: values.html,
        type: 'share',
        fileName: values.fileName || 'share.pdf',
      });
      return result;
    },
  },
  {
    id: 'url-base64',
    label: 'Generate from URL',
    description: 'Load a remote page and render it as a PDF (base64).',
    inputs: [
      {
        name: 'url',
        label: 'URL',
        type: 'text',
        value: 'https://example.com',
      },
      {
        name: 'fileName',
        label: 'File name',
        type: 'text',
        value: 'url.pdf',
      },
    ],
    run: async (values) => {
      if (!values.url) {
        throw new Error('Provide a URL to render.');
      }
      const result = await plugin.fromURL({
        url: values.url,
        type: 'base64',
        fileName: values.fileName || 'url.pdf',
      });
      return result;
    },
  },
];

const actionSelect = document.getElementById('action-select');
const formContainer = document.getElementById('action-form');
const descriptionBox = document.getElementById('action-description');
const runButton = document.getElementById('run-action');
const output = document.getElementById('plugin-output');

function buildForm(action) {
  formContainer.innerHTML = '';
  if (!action.inputs || !action.inputs.length) {
    const note = document.createElement('p');
    note.className = 'no-input-note';
    note.textContent = 'This action does not require any inputs.';
    formContainer.appendChild(note);
    return;
  }

  action.inputs.forEach((input) => {
    const fieldWrapper = document.createElement('div');
    fieldWrapper.className = input.type === 'checkbox' ? 'form-field inline' : 'form-field';

    const label = document.createElement('label');
    label.textContent = input.label;
    label.htmlFor = `field-${input.name}`;

    let field;
    switch (input.type) {
      case 'textarea': {
        field = document.createElement('textarea');
        field.rows = input.rows || 4;
        if (input.value) field.value = input.value;
        break;
      }
      case 'select': {
        field = document.createElement('select');
        (input.options || []).forEach((option) => {
          const opt = document.createElement('option');
          opt.value = option.value;
          opt.textContent = option.label;
          if (input.value !== undefined && option.value === input.value) {
            opt.selected = true;
          }
          field.appendChild(opt);
        });
        break;
      }
      case 'checkbox': {
        field = document.createElement('input');
        field.type = 'checkbox';
        field.checked = Boolean(input.value);
        break;
      }
      default: {
        field = document.createElement('input');
        field.type = input.type || 'text';
        if (input.value !== undefined && input.value !== null) {
          field.value = String(input.value);
        }
      }
    }

    field.id = `field-${input.name}`;
    field.name = input.name;
    field.dataset.type = input.type || 'text';

    if (input.placeholder && input.type !== 'checkbox') {
      field.placeholder = input.placeholder;
    }

    if (input.type === 'checkbox') {
      fieldWrapper.appendChild(field);
      fieldWrapper.appendChild(label);
    } else {
      fieldWrapper.appendChild(label);
      fieldWrapper.appendChild(field);
    }

    formContainer.appendChild(fieldWrapper);
  });
}

function getFormValues(action) {
  const values = {};
  (action.inputs || []).forEach((input) => {
    const field = document.getElementById(`field-${input.name}`);
    if (!field) return;
    switch (input.type) {
      case 'number': {
        values[input.name] = field.value === '' ? null : Number(field.value);
        break;
      }
      case 'checkbox': {
        values[input.name] = field.checked;
        break;
      }
      default: {
        values[input.name] = field.value;
      }
    }
  });
  return values;
}

function setAction(action) {
  descriptionBox.textContent = action.description || '';
  buildForm(action);
  output.textContent = 'Ready to run the selected action.';
}

function populateActions() {
  actionSelect.innerHTML = '';
  actions.forEach((action) => {
    const option = document.createElement('option');
    option.value = action.id;
    option.textContent = action.label;
    actionSelect.appendChild(option);
  });
  setAction(actions[0]);
}

actionSelect.addEventListener('change', () => {
  const action = actions.find((item) => item.id === actionSelect.value);
  if (action) {
    setAction(action);
  }
});

runButton.addEventListener('click', async () => {
  const action = actions.find((item) => item.id === actionSelect.value);
  if (!action) return;
  const values = getFormValues(action);
  try {
    const result = await action.run(values);
    if (result === undefined) {
      output.textContent = 'Action completed.';
    } else if (typeof result === 'string') {
      output.textContent = result;
    } else {
      output.textContent = JSON.stringify(result, null, 2);
    }
  } catch (error) {
    output.textContent = `Error: ${error?.message ?? error}`;
  }
});

populateActions();
