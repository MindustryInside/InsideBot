package inside.command.common;

import com.fasterxml.jackson.databind.JsonNode;
import discord4j.common.ReactorResources;
import inside.command.Command;
import inside.command.model.*;
import inside.data.entity.GuildConfig;
import inside.util.Lazy;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.util.*;

import static inside.util.ContextUtil.KEY_REPLY;

@DiscordCommand(key = {"translate", "tr"}, params = "command.translate.params", description = "command.translate.description")
public class TranslateCommand extends Command{
    private static final String languages = """
            Afrikaans (`af`),
            Albanian (`sq`), Amharic (`am`), Arabic (`ar`), Armenian (`hy`), Automatic (`auto`),
            Azerbaijani (`az`), Basque (`eu`), Belarusian (`be`), Bengali (`bn`), Bosnian (`bs`),
            Bulgarian (`bg`), Catalan (`ca`), Cebuano (`ceb`), Chichewa (`ny`), Chinese Simplified (`zh-cn`),
            Chinese Traditional (`zh-tw`), Corsican (`co`), Croatian (`hr`), Czech (`cs`), Danish (`da`),
            Dutch (`nl`), English (`en`), Esperanto (`eo`), Estonian (`et`), Filipino (`tl`),
            Finnish (`fi`), French (`fr`), Frisian (`fy`), Galician (`gl`), Georgian (`ka`),
            German (`de`), Greek (`el`), Gujarati (`gu`), Haitian Creole (`ht`), Hausa (`ha`),
            Hawaiian (`haw`), Hebrew (`iw`), Hindi (`hi`), Hmong (`hmn`), Hungarian (`hu`),
            Icelandic (`is`), Igbo (`ig`), Indonesian (`id`), Irish (`ga`), Italian (`it`),
            Japanese (`ja`), Javanese (`jw`), Kannada (`kn`), Kazakh (`kk`), Khmer (`km`),
            Korean (`ko`), Kurdish (Kurmanji) (`ku`), Kyrgyz (`ky`), Lao (`lo`), Latin (`la`),
            Latvian (`lv`), Lithuanian (`lt`), Luxembourgish (`lb`), Macedonian (`mk`), Malagasy (`mg`),
            Malay (`ms`), Malayalam (`ml`), Maltese (`mt`), Maori (`mi`), Marathi (`mr`),
            Mongolian (`mn`), Myanmar (Burmese) (`my`), Nepali (`ne`), Norwegian (`no`), Pashto (`ps`),
            Persian (`fa`), Polish (`pl`), Portuguese (`pt`), Punjabi (`ma`), Romanian (`ro`),
            Russian (`ru`), Samoan (`sm`), Scots Gaelic (`gd`), Serbian (`sr`), Sesotho (`st`),
            Shona (`sn`), Sindhi (`sd`), Sinhala (`si`), Slovak (`sk`), Slovenian (`sl`),
            Somali (`so`), Spanish (`es`), Sundanese (`su`), Swahili (`sw`), Swedish (`sv`),
            Tajik (`tg`), Tamil (`ta`), Telugu (`te`), Thai (`th`), Turkish (`tr`),
            Ukrainian (`uk`), Urdu (`ur`), Uzbek (`uz`), Vietnamese (`vi`), Welsh (`cy`),
            Xhosa (`xh`), Yiddish (`yi`), Yoruba (`yo`), Zulu (`zu`)
            """;

    private final Lazy<HttpClient> httpClient = Lazy.of(ReactorResources.DEFAULT_HTTP_CLIENT);

    @Override
    public Mono<Void> execute(CommandEnvironment env, CommandInteraction interaction){
        String from = interaction.getOption(0)
                .flatMap(CommandOption::getValue)
                .map(OptionValue::asString)
                .orElseThrow(IllegalStateException::new);

        String to = interaction.getOption(1)
                .flatMap(CommandOption::getValue)
                .map(OptionValue::asString)
                .orElseThrow(IllegalStateException::new);

        String text = interaction.getOption(2)
                .flatMap(CommandOption::getValue)
                .map(OptionValue::asString)
                .orElseThrow(IllegalStateException::new);

        Map<String, Object> query = Map.of(
                "client", "dict-chrome-ex",
                "dt", "t", "ie", "UTF-8", "oe", "UTF-8",
                "q", text, "tl", to, "sl", from
        );

        return httpClient.get().get().uri(CommandUtils.expandQuery("https://translate.google.com/translate_a/t", query))
                .responseSingle((res, buf) -> buf.asString().flatMap(byteBuf -> Mono.fromCallable(() ->
                        env.message().getClient().rest().getCoreResources().getJacksonResources()
                                .getObjectMapper().readTree(byteBuf))))
                .flatMap(node -> Mono.justOrEmpty(Optional.ofNullable(node.get("sentences"))
                        .map(arr -> arr.get(0))
                        .map(single -> single.get("trans"))
                        .map(JsonNode::asText)))
                .switchIfEmpty(messageService.err(env, "command.translate.incorrect-language").then(Mono.never()))
                .flatMap(str -> messageService.text(env, str)
                        .withMessageReference(env.message().getId()))
                .then();
    }

    @Override
    public Mono<Void> help(CommandEnvironment env, String prefix){
        return messageService.infoTitled(env, "command.help.title", "command.translate.help",
                GuildConfig.formatPrefix(prefix), languages)
                .then();
    }
}
