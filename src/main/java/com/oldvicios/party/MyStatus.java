package com.oldvicios.party;

import java.util.Collections;
import java.util.List;

/** Resposta do GET /v1/plugin/party/me: meu papel, meu grupo e avisos desde a última consulta. */
final class MyStatus
{
	enum Role
	{
		NONE, LEADER, MEMBER, APPLICANT
	}

	static final class Notice
	{
		/** ACCEPTED, DECLINED, KICKED, CLOSED, EXPIRED ou PARTY_FULL. */
		String type;
		String partyName;

		/** Texto para o chat; null para tipos que esta versão não conhece. */
		String message()
		{
			String party = "\"" + partyName + "\"";
			switch (type != null ? type : "")
			{
				case "ACCEPTED":
					return "Você entrou no grupo " + party;
				case "DECLINED":
					return "Sua candidatura no grupo " + party + " foi recusada";
				case "KICKED":
					return "Você foi removido do grupo " + party;
				case "CLOSED":
					return "O grupo " + party + " foi encerrado";
				case "EXPIRED":
					return "O grupo " + party + " foi encerrado (líder inativo)";
				case "PARTY_FULL":
					return "O grupo " + party + " encheu. Você continua na fila";
				default:
					return null;
			}
		}
	}

	Role role;
	/** O grupo em que estou (líder, membro ou candidato); null se nenhum. */
	Party party;
	List<Notice> notices;

	Role role()
	{
		return role != null ? role : Role.NONE;
	}

	List<Notice> notices()
	{
		return notices != null ? notices : Collections.emptyList();
	}
}
