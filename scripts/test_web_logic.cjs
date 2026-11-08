const fs = require('fs');
const html = fs.readFileSync('app/src/main/assets/index.html', 'utf8');
const scripts = [...html.matchAll(/<script[^>]*>([\s\S]*?)<\/script>/gi)].map(m => m[1]);
if (!scripts.length) throw new Error('No inline application script');
const source = scripts.join('\n');
const match = source.match(/function validBackup\(x\)\{[\s\S]*?\n\}/);
if (!match) throw new Error('validBackup function not found');
const parseLocalMatch = source.match(/function parseLocalDateTime\([^\n]*\)[\s\S]*?(?=\nfunction |$)/);
if (!parseLocalMatch) throw new Error('parseLocalDateTime function not found');
const validBackup = new Function(`${parseLocalMatch[0]}; ${match[0]}; return validBackup;`)();
const now = new Date().toISOString();
const good = {
  glucose: [{ id: 'g1', value: 126, when: now, context: 'Ao acordar' }],
  insulin: [{ id: 'i1', name: 'Insulina de exemplo', status: 'applied', units: 8, when: now }],
  meds: [{ id: 'm1', name: 'Medicamento de exemplo', dose: '500 mg', time: '08:00' }],
  medEvents: [{ id: 'e1', medId: 'm1', medName: 'Medicamento de exemplo', when: now }],
  routine: [{ id: 'r1', type: 'reminder', title: 'Lembrete de exemplo', repeat: 'Todos os dias', triggerAt: now }],
  appointments: [{ id: 'a1', type: 'consultation', title: 'Consulta de exemplo', when: new Date(Date.now() + 86400000).toISOString().slice(0,16), professional: 'Profissional', location: 'Clínica', notes: '', reminderMinutes: 30, reminderAt: new Date(Date.now() + 84600000).toISOString(), precision: 'approximate', done: false }],
  profile: { displayName: 'Teste', mode: 'easy', contrast: 'high' }
};
const assert = (condition, message) => { if (!condition) throw new Error(message); };

const calendarFunctionNames = ['localDayKey', 'parseLocalDateTime', 'appointmentReminderInstant', 'calendarCells', 'appointmentDateKey'];
const calendarSource = calendarFunctionNames.map(name => {
  const rx = new RegExp(`function ${name}\\([^\\n]*\\)[\\s\\S]*?(?=\\nfunction |$)`);
  const m = source.match(rx);
  if (!m) throw new Error(`Calendar function not found: ${name}`);
  return m[0];
}).join('\n');
const calendarApi = new Function(`${calendarSource}; return {localDayKey, parseLocalDateTime, appointmentReminderInstant, calendarCells, appointmentDateKey};`)();
const parsedLocal = calendarApi.parseLocalDateTime('2026-11-07T10:45');
const reminderIso = calendarApi.appointmentReminderInstant('2026-11-07T10:45', 30);
assert(Number.isFinite(Date.parse(reminderIso)), 'Appointment reminder instant could not be derived');
assert(calendarApi.appointmentReminderInstant('2026-11-07T10:45', 7) === '', 'Unsupported reminder offset accepted');
assert(parsedLocal instanceof Date && parsedLocal.getHours() === 10 && parsedLocal.getMinutes() === 45, 'Local appointment date/time parsed incorrectly');
assert(Date.parse(reminderIso) === parsedLocal.getTime() - 30 * 60000, 'Reminder offset does not match selected local appointment time');
assert(calendarApi.parseLocalDateTime('2026-02-31T10:00') === null, 'Impossible local appointment date accepted');
const cells = calendarApi.calendarCells(2026, 10);
assert(cells.length === 42, 'Calendar month should render six complete weeks');
assert(cells.some(day => day.key === '2026-11-07' && day.inMonth), 'Calendar did not include selected month day');
assert(calendarApi.appointmentDateKey('2026-11-07T10:45') === '2026-11-07', 'Appointment day key mismatch');
const normalizeMatch = source.match(/function normalizeData\(x\)\{[\s\S]*?(?=\nfunction load\()/);
if (!normalizeMatch) throw new Error('normalizeData function not found');
const normalize = new Function(`${parseLocalMatch[0]}; function defaults(){return {glucose:[],insulin:[],meds:[],medEvents:[],routine:[],appointments:[],profile:{displayName:'',mode:'standard',contrast:'normal'}}}; ${normalizeMatch[0]}; return normalizeData;`)();
const normalized = normalize({ appointments: good.appointments.concat([{...good.appointments[0]}]), profile:{displayName:'Teste'} });
assert(normalized.appointments.length === 1, 'Normalizer failed to remove duplicate appointment IDs');
assert(normalized.profile.displayName === 'Teste', 'Normalizer lost profile');
const normalizedBad = normalize({ appointments:[{...good.appointments[0], when:'2026-02-31T10:00'}] });
assert(normalizedBad.appointments.length === 0, 'Normalizer retained impossible appointment date');

assert(validBackup(good), 'Valid backup rejected');
let bad = structuredClone(good); bad.insulin[0].units = 900;
assert(!validBackup(bad), 'Out-of-range insulin record accepted');
bad = structuredClone(good); bad.insulin[0].units = 0;
assert(!validBackup(bad), 'Zero applied insulin units accepted');
bad = structuredClone(good); bad.insulin[0].status = 'not_applied'; bad.insulin[0].units = 8;
assert(!validBackup(bad), 'Units accepted for a not-applied insulin record');
bad = structuredClone(good); bad.insulin[0].status = 'uncertain'; bad.insulin[0].units = 8;
assert(!validBackup(bad), 'Units accepted for an uncertain insulin record');
bad = structuredClone(good); bad.insulin[0].status = undefined; bad.insulin[0].units = 8;
assert(validBackup(bad), 'Legacy insulin record without status was not accepted');
bad = structuredClone(good); bad.insulin[0].status = 'mystery';
assert(!validBackup(bad), 'Unknown insulin status accepted');
bad = structuredClone(good); bad.glucose[0].value = 1200;
assert(!validBackup(bad), 'Out-of-range glucose accepted');
bad = structuredClone(good); bad.glucose[0].when = new Date(Date.now() + 60 * 60 * 1000).toISOString();
assert(!validBackup(bad), 'Future glucose record accepted');
bad = { ...good, meds: 'not-an-array' };
assert(!validBackup(bad), 'Invalid backup structure accepted');
bad = structuredClone(good); bad.profile.mode = 'hidden-admin';
assert(!validBackup(bad), 'Unknown profile mode accepted');
bad = structuredClone(good); bad.medEvents = new Array(10001).fill(good.medEvents[0]);
assert(!validBackup(bad), 'Excessive medication history accepted');
bad = structuredClone(good); bad.routine[0].repeat = 'every-minute';
assert(!validBackup(bad), 'Unknown reminder recurrence accepted');
bad = structuredClone(good); bad.appointments[0].type = 'unknown';
assert(!validBackup(bad), 'Unknown appointment type accepted');
bad = structuredClone(good); bad.appointments[0].reminderMinutes = 7;
assert(!validBackup(bad), 'Unsupported appointment reminder offset accepted');
bad = structuredClone(good); bad.appointments[0].precision = 'instant-always';
assert(!validBackup(bad), 'Unknown appointment alarm precision accepted');
bad = structuredClone(good); bad.appointments[0].title = 'x'.repeat(121);
assert(!validBackup(bad), 'Overlong appointment title accepted');
bad = structuredClone(good); bad.appointments[0].when = 'not-a-date';
assert(!validBackup(bad), 'Invalid appointment date accepted');
bad = structuredClone(good); bad.appointments[0].when = '2026-02-31T10:00';
assert(!validBackup(bad), 'Impossible local appointment date accepted');
bad = structuredClone(good); bad.appointments.push({ ...good.appointments[0] });
assert(!validBackup(bad), 'Duplicate appointment IDs accepted');
bad = structuredClone(good); bad.appointments[0].reminderAt = 'not-an-instant';
assert(!validBackup(bad), 'Invalid appointment reminder instant accepted');
bad = structuredClone(good); bad.appointments[0].done = 'yes';
assert(!validBackup(bad), 'Non-boolean appointment completion status accepted');

const transactionStart = source.indexOf('function commitData(mutator){');
const transactionEnd = source.indexOf('function esc(', transactionStart);
if (transactionStart < 0 || transactionEnd < 0) throw new Error('commitData function not found');
const transactionFunction = source.slice(transactionStart, transactionEnd).trim();
const makeTransaction = new Function('initial', 'saveResult', `let db = structuredClone(initial); let renderCount = 0; let toastCount = 0; function save(){ return saveResult; } function render(){ renderCount++; } function toast(){ toastCount++; } ${transactionFunction} return { commitData, getDb: () => db, getRenderCount: () => renderCount, getToastCount: () => toastCount };`);
let tx = makeTransaction({ items: ['before'] }, false);
assert(tx.commitData(() => tx.getDb().items.push('not-saved')) === false, 'Failed persistence reported as successful');
assert(JSON.stringify(tx.getDb()) === JSON.stringify({ items: ['before'] }), 'Failed persistence did not roll back in-memory data');
assert(tx.getRenderCount() === 1, 'Failed persistence did not refresh after rollback');
tx = makeTransaction({ items: ['before'] }, true);
assert(tx.commitData(() => tx.getDb().items.push('saved')) === true, 'Successful persistence reported as failed');
assert(JSON.stringify(tx.getDb()) === JSON.stringify({ items: ['before', 'saved'] }), 'Successful persistence did not keep mutation');
console.log('WEB LOGIC TESTS PASSED: backup validation, insulin state, dose limits, future records, profile, recurrence, appointment schema, strict dates, duplicate-ID rejection, safe data normalization, timezone-aware reminder calculation, local date parsing, six-week calendar grid, collection limits and rollback-on-save-failure.');
