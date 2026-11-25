// Posta no Discord (#🪵┃plugin-changelog) o changelog da versão que está publicada no Plugin Hub.
// Roda no workflow .github/workflows/changelog.yml. Sem dependências (Node 20+).
//
// 1. Lê no runelite/plugin-hub o commit publicado do OldVicios e a data do merge.
// 2. Lê a versão (runelite-plugin.properties) desse commit.
// 3. Se já existe a release vX.Y.Z publicada, a versão já foi postada: não faz nada.
// 4. Senão monta a mensagem com a seção "## OldVicios X.Y.Z" do CHANGELOG.md, posta pelo
//    webhook e publica a release (rascunho antes de postar, para nunca postar duas vezes).
//
// Env: GITHUB_TOKEN, GITHUB_REPOSITORY, CHANGELOG_WEBHOOK_URL, DRY_RUN=true (só mostra).

import { readFileSync } from 'node:fs';
import { pathToFileURL } from 'node:url';

const HUB_REPO = 'runelite/plugin-hub';
const HUB_FILE = 'plugins/oldvicios';
const DISCORD_LIMIT = 2000;
const USER_AGENT = 'OldVicios-Changelog (https://github.com/oldviciosOSRS/OldVicios, 1.0)';

/** Corpo da seção "## OldVicios <versão>" do CHANGELOG.md, sem o título; null se não existir. */
export function changelogSection(markdown, version) {
  const lines = markdown.split(/\r?\n/);
  const start = lines.findIndex((l) => l.trim() === `## OldVicios ${version}`);
  if (start === -1) return null;
  let end = lines.findIndex((l, i) => i > start && l.startsWith('## '));
  if (end === -1) end = lines.length;
  const body = lines.slice(start + 1, end);
  while (body.length && !body[0].trim()) body.shift();
  while (body.length && !body[body.length - 1].trim()) body.pop();
  return body.join('\n');
}

/** dd/mm/aaaa no horário de Brasília. */
export function formatDate(iso) {
  return new Intl.DateTimeFormat('pt-BR', {
    timeZone: 'America/Sao_Paulo',
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  }).format(new Date(iso));
}

/**
 * Mensagem no formato do canal:
 *   ## OldVicios X.Y.Z
 *   *Publicada em dd/mm/aaaa*            (ou "*Publicada em dd/mm/aaaa · nota*")
 *   <linha em branco>
 *   **Adicionado** / **Modificado** / **Corrigido** com itens em "-"
 * A nota opcional é uma linha em itálico logo abaixo do título da seção no CHANGELOG.md.
 */
export function buildMessage(version, publishedAt, section) {
  const lines = section.split('\n');
  let note = '';
  const italic = lines[0] && lines[0].match(/^\*(?!\*)(.+)\*$/);
  if (italic) {
    note = italic[1];
    lines.shift();
    while (lines.length && !lines[0].trim()) lines.shift();
  }
  if (!lines.join('').trim()) throw new Error(`A seção da versão ${version} no CHANGELOG.md está vazia`);
  const date = `*Publicada em ${formatDate(publishedAt)}${note ? ` · ${note}` : ''}*`;
  return `## OldVicios ${version}\n${date}\n\n${lines.join('\n')}`;
}

async function github(path, { method = 'GET', body } = {}) {
  const res = await fetch(`https://api.github.com${path}`, {
    method,
    headers: {
      Authorization: `Bearer ${process.env.GITHUB_TOKEN}`,
      Accept: 'application/vnd.github+json',
      'X-GitHub-Api-Version': '2022-11-28',
      'User-Agent': USER_AGENT,
      ...(body ? { 'Content-Type': 'application/json' } : {}),
    },
    body: body ? JSON.stringify(body) : undefined,
  });
  if (!res.ok) throw new Error(`GitHub ${method} ${path}: ${res.status} ${await res.text()}`);
  return res.status === 204 ? null : res.json();
}

const decode = (file) => Buffer.from(file.content, 'base64').toString('utf8');

async function main() {
  const repo = process.env.GITHUB_REPOSITORY;
  const dryRun = process.env.DRY_RUN === 'true';
  if (!repo || !process.env.GITHUB_TOKEN) throw new Error('Faltam GITHUB_REPOSITORY e/ou GITHUB_TOKEN');

  // 1. O que está publicado no Plugin Hub (último commit que mexeu no manifesto = merge do PR)
  const [hubCommit] = await github(`/repos/${HUB_REPO}/commits?path=${HUB_FILE}&per_page=1`);
  const publishedAt = hubCommit.commit.committer.date;
  const manifest = decode(await github(`/repos/${HUB_REPO}/contents/${HUB_FILE}?ref=${hubCommit.sha}`));
  const commit = manifest.match(/^commit=([0-9a-f]{40})\s*$/m)?.[1];
  if (!commit) throw new Error(`Não achei "commit=" em ${HUB_FILE} do Plugin Hub`);

  // 2. Versão desse commit
  const props = decode(await github(`/repos/${repo}/contents/runelite-plugin.properties?ref=${commit}`));
  const version = props.match(/^version=(.+)$/m)?.[1]?.trim();
  if (!version) throw new Error(`runelite-plugin.properties do commit ${commit} não tem "version="`);
  const tag = `v${version}`;
  console.log(`Plugin Hub publica a ${version} (commit ${commit.slice(0, 7)}, merge em ${publishedAt})`);

  // 3. Já postada?
  const releases = await github(`/repos/${repo}/releases?per_page=100`);
  const release = releases.find((r) => r.tag_name === tag);

  const section = changelogSection(readFileSync('CHANGELOG.md', 'utf8'), version);
  if (section === null) {
    if (release && !release.draft) return console.log(`Release ${tag} já existe: nada a postar.`);
    throw new Error(
      `CHANGELOG.md não tem a seção "## OldVicios ${version}". Adicione na main; o workflow tenta de novo na próxima hora.`,
    );
  }
  const content = buildMessage(version, publishedAt, section);
  if (content.length > DISCORD_LIMIT) {
    throw new Error(`A mensagem da ${version} tem ${content.length} caracteres (limite do Discord: ${DISCORD_LIMIT})`);
  }

  if (release && !release.draft) {
    console.log(`Release ${tag} já existe: a ${version} já foi postada.`);
    if (dryRun) console.log(`\n--- mensagem ---\n${content}`);
    return;
  }
  if (release && release.draft) {
    throw new Error(
      `Existe um rascunho da release ${tag}: uma execução anterior parou no meio. Veja no canal se a mensagem saiu. ` +
        'Se saiu, publique o rascunho; se não, apague-o e o workflow posta na próxima hora.',
    );
  }
  if (dryRun) {
    console.log(`DRY_RUN: postaria isto e criaria a release ${tag}:\n\n--- mensagem ---\n${content}`);
    return;
  }

  // 4. Rascunho -> posta -> publica. Se o Discord falhar, apaga o rascunho e tenta na próxima hora.
  const webhook = process.env.CHANGELOG_WEBHOOK_URL;
  if (!webhook) throw new Error('Falta o secret CHANGELOG_WEBHOOK_URL');
  const draft = await github(`/repos/${repo}/releases`, {
    method: 'POST',
    body: {
      tag_name: tag,
      target_commitish: commit,
      name: `OldVicios ${version}`,
      body: content.split('\n').slice(1).join('\n'),
      draft: true,
    },
  });
  try {
    const res = await fetch(`${webhook}?wait=true`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'User-Agent': USER_AGENT },
      body: JSON.stringify({ content, allowed_mentions: { parse: [] } }),
    });
    if (!res.ok) throw new Error(`Discord respondeu ${res.status}: ${await res.text()}`);
    console.log(`Postado no Discord (mensagem ${(await res.json()).id})`);
  } catch (error) {
    await github(`/repos/${repo}/releases/${draft.id}`, { method: 'DELETE' });
    throw error;
  }
  await github(`/repos/${repo}/releases/${draft.id}`, { method: 'PATCH', body: { draft: false } });
  console.log(`Release ${tag} publicada`);
}

if (import.meta.url === pathToFileURL(process.argv[1]).href) {
  main().catch((error) => {
    console.error(error.message);
    process.exit(1);
  });
}
