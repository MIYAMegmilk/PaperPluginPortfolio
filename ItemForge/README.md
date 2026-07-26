# ItemForge

GUI式のアイテム進化システム。武器と素材をGUIに配置して上位アイテムへ進化させる。進化時にトレイトが [ItemTraits](../ItemTraits) APIを通じて引き継がれる。

## 動作環境

- Paper 1.21.1
- Java 21
- [ItemTraits](../ItemTraits) プラグイン

## 導入方法

1. ビルド: `./gradlew build`
2. `build/libs/ItemForge-1.0.0.jar` を `ItemTraits-1.0.0.jar` と一緒に `plugins/` に配置
3. サーバー起動で `config.yml` が自動生成される
4. `config.yml` でレシピを定義

## コマンド

| コマンド | 権限 | 説明 |
|---------|------|------|
| `/forge` | `itemforge.use`（デフォルト: 全員） | 進化合成GUIを開く |

## config.yml

```yaml
recipes:
  iron_to_diamond:
    input: IRON_SWORD                  # 入力アイテムのMaterial
    output:
      material: DIAMOND_SWORD         # 出力アイテムのMaterial
      name: "&bEvolved Diamond Blade"  # 表示名
      lore:                            # ロア
        - "&7Forged from iron through evolution."
    materials:                         # 必要素材（MATERIAL:個数）
      - "DIAMOND:3"
      - "BLAZE_POWDER:2"
```

## GUIレイアウト

```
行0:  黒ガラス枠
行1:  [枠] [入力スロット] [枠] [矢印/状態] [枠] [出力プレビュー] [枠] [枠] [枠]
行2:  黒ガラス枠
行3:  [枠] [素材1] [素材2] [素材3] [枠] [確認ボタン] [枠] [枠] [枠]
行4:  黒ガラス枠
```

- **入力スロット**: 進化させたい武器を配置
- **素材スロット**: レシピに必要な素材を配置（最大3種）
- **出力プレビュー**: レシピがマッチすると進化後アイテムのプレビューを表示
- **確認ボタン**: クリックで進化を実行。素材が消費され、入力が出力に置き換わる
- **GUI終了時**: 入力・素材スロットのアイテムはプレイヤーに返却される（消失しない）

## 設計

- **EvolutionRecipe / Ingredient** — configからパースされる不変レコード
- **ForgeGui** — チェストGUI。枠・入力・素材・出力・確認スロットの管理、InventoryClickEventハンドリング
- **ItemForgePlugin** — config読み込み、ServicesManager経由のAPI取得、リスナーとコマンドの登録
- **プラグイン間連携** — `compileOnly` でItemTraitsに依存。実行時はBukkitの `ServicesManager` でAPI取得

## ライセンス

MIT
