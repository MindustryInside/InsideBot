package inside.interaction.support;

import discord4j.discordjson.json.*;

import java.util.List;

final class RegistrarUtil{

    private RegistrarUtil(){}

    public static boolean isChanged(ApplicationCommandData oldCommand, ApplicationCommandRequest newCommand){
        return !oldCommand.type().toOptional().orElse(1).equals(newCommand.type().toOptional().orElse(1))
                || !oldCommand.description().equals(newCommand.description().toOptional().orElse(""))
                || oldCommand.defaultPermission().toOptional().orElse(true)
                != newCommand.defaultPermission().toOptional().orElse(true)
                || !isChanged(oldCommand.options().toOptional().orElse(List.of()),
                newCommand.options().toOptional().orElse(List.of()));
    }

    private static boolean isChanged(List<ApplicationCommandOptionData> oldOptions, List<ApplicationCommandOptionData> newOptions){
        if(oldOptions.size() != newOptions.size()){
            return true;
        }

        for(int i = 0; i < oldOptions.size(); i++){
            var oldOption = oldOptions.get(i);
            var newOption = newOptions.get(i);
            if(isChanged(oldOption, newOption)){
                return true;
            }
        }

        return false;
    }

    private static boolean isChanged(ApplicationCommandOptionData oldOption, ApplicationCommandOptionData newOption){
        return !oldOption.name().equals(newOption.name())
                || !oldOption.description().equals(newOption.description())
                || oldOption.type() != newOption.type()
                || oldOption.required().toOptional().orElse(false) != newOption.required()
                .toOptional().orElse(false)
                || oldOption.autocomplete().toOptional().orElse(false) != newOption.autocomplete()
                .toOptional().orElse(false)
                || !oldOption.channelTypes().toOptional().orElse(List.of()).equals(newOption.channelTypes()
                .toOptional().orElse(List.of()))
                || !oldOption.maxValue().toOptional().orElse(0d).equals(newOption.maxValue()
                .toOptional().orElse(0d))
                || !oldOption.minValue().toOptional().orElse(0d).equals(newOption.minValue()
                .toOptional().orElse(0d))
                || !oldOption.choices().toOptional().orElse(List.of()).equals(newOption.choices()
                .toOptional().orElse(List.of()))
                || isChanged(oldOption.options().toOptional().orElse(List.of()),
                newOption.options().toOptional().orElse(List.of()));
    }
}
