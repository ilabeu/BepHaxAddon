package bep.hax.gui.screens;

import bep.hax.modules.Minesweeper;
import org.jetbrains.annotations.Nullable;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.utils.Cell;
import meteordevelopment.meteorclient.gui.WindowScreen;
import bep.hax.gui.widgets.minesweeper.WMinesweeper;
import meteordevelopment.meteorclient.gui.widgets.WWidget;

/**
 * @author Tas [0xTas] <root@0xTas.dev>
 **/
public class MinesweeperScreen extends WindowScreen {
    private final GuiTheme theme;
    private final Minesweeper module;
    private @Nullable Cell<? extends WWidget> widget = null;

    public MinesweeperScreen(Minesweeper module, GuiTheme theme, String title) {
        super(theme, title);
        this.theme = theme;
        this.module = module;
    }

    @Override
    public void initWidgets() {
        widget = add(new WMinesweeper(module, theme));
    }

    @Override
    public void onClosed() {
        if (module.isActive()) module.toggle();
        if (widget != null && widget.widget() instanceof WMinesweeper minesweeper) {
            if (module.shouldSave.get() && minesweeper.shouldSaveGame()) {
                module.saveGame(minesweeper.saveGame());
            }
        }
    }
}
